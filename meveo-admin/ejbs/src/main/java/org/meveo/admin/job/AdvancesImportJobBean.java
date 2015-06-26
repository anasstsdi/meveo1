package org.meveo.admin.job;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ImportInvoiceException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.cache.CdrEdrProcessingCacheContainerProvider;
import org.meveo.commons.utils.FileUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.BaseEntity;
import org.meveo.model.IProvider;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.mediation.Access;
import org.meveo.model.mediation.CDRRejectionCauseEnum;
import org.meveo.model.payments.MatchingStatusEnum;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.model.rating.EDR;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.billing.impl.UsageRatingService;
import org.meveo.service.medina.impl.ADRParsingException;
import org.meveo.service.medina.impl.EDRDAO;
import org.meveo.service.medina.impl.InvalidAccessException;
import org.meveo.service.medina.impl.InvalidAdrAccessException;
import org.meveo.service.medina.impl.InvalidAdrFormatException;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class AdvancesImportJobBean {

	@Inject
	private Logger log;

	@Inject
	private SubscriptionService subscriptionService;

	@Inject
	private UsageRatingService usageRatingService;

	@Inject
	private CdrEdrProcessingCacheContainerProvider cdrEdrProcessingCacheContainerProvider;

	@Inject
	private EdrService edrService;

	@Inject
	private AccountOperationService accountOperationService;

	@Inject
	private OCCTemplateService oCCTemplateService;

	@Inject
	private ParamBean paramBean;

	private String outputDir;
	private String rejectDir;
	private String report;
	private String adrFileName;

	private PrintWriter rejectFileWriter;
	private PrintWriter outputFileWriter;

	private ADRParsingService aDRParsingService = new ADRParsingService();
	private ADRParser adrParser = new ADRParser();

	@Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void execute(JobExecutionResultImpl result, String parameter, User currentUser, File file, int dayOfMonth, String accountCode, int dueDateDelay) {
		Provider provider = currentUser.getProvider();

		ParamBean parambean = ParamBean.getInstance();
		String advancesDir = parambean.getProperty("providers.rootDir", "/tmp/meveo/") + File.separator + provider.getCode() + File.separator + "imports" + File.separator
				+ "advances" + File.separator;

		outputDir = advancesDir + "output";
		rejectDir = advancesDir + "reject";

		File f = new File(outputDir);
		if (!f.exists()) {
			f.mkdirs();
		}
		f = new File(rejectDir);
		if (!f.exists()) {
			f.mkdirs();
		}
		report = "";

		if (file != null) {
			adrParser.init(file);

			adrFileName = file.getAbsolutePath();
			result.setNbItemsToProcess(1);

			log.info("InputFiles job {} in progress...", file.getName());

			adrFileName = file.getName();
			File currentFile = FileUtils.addExtension(file, ".processing");
			BufferedReader adrReader = null;
			try {
				adrReader = new BufferedReader(new InputStreamReader(new FileInputStream(currentFile)));
				String line = null;
				int processed = 0;

				BigDecimal totalAmountWithoutTax = new BigDecimal(0);
				Subscription subscription = null;
				while ((line = adrReader.readLine()) != null) {
					processed++;
					try {
						WalletOperation walletOperation = new WalletOperation();
						EDR edr = rateEdr(walletOperation, line, currentUser, dayOfMonth);
						if (subscription == null) {
							subscription = edr.getSubscription();
						}
						totalAmountWithoutTax = totalAmountWithoutTax.add(walletOperation.getAmountWithTax());
						outputADR(line);
						result.registerSucces();
					} catch (ADRParsingException e) {
						log.warn(e.getMessage());
						result.registerError("file=" + file.getName() + ", line=" + processed + ": " + e.getRejectionCause().name());
						rejectADR(e.getAdr(), e.getRejectionCause());
					} catch (Exception e) {
						log.error(e.getMessage());
						result.registerError("file=" + file.getName() + ", line=" + processed + ": " + e.getMessage());
						rejectADR(line, CDRRejectionCauseEnum.TECH_ERR);
					}
				}

				log.debug("totalAmountWithoutTax={}", totalAmountWithoutTax);
				createAccountOperation(subscription, totalAmountWithoutTax, accountCode, dueDateDelay, currentUser);

				if (processed == 0) {
					report += "\r\n file is empty ";
				}

				log.info("InputFiles job {} done.", file.getName());
				result.registerSucces();
			} catch (Exception e) {
				log.error("Failed to process ADR file {}", file.getName(), e);
				result.registerError(e.getMessage());
				FileUtils.moveFile(rejectDir, currentFile, file.getName());
			} finally {
				try {
					if (adrReader != null) {
						adrReader.close();
					}
				} catch (Exception e) {
					log.error("Failed to close ADR reader for file {}", file.getName(), e);
				}

				try {
					if (currentFile != null) {
						currentFile.delete();
					}
				} catch (Exception e) {
					report += "\r\n cannot delete " + adrFileName;
				}

				try {
					if (rejectFileWriter != null) {
						rejectFileWriter.close();
						rejectFileWriter = null;
					}
				} catch (Exception e) {
					log.error("Failed to close rejected ADR writer for file {}", file.getName(), e);
				}

				try {
					if (outputFileWriter != null) {
						outputFileWriter.close();
						outputFileWriter = null;
					}
				} catch (Exception e) {
					log.error("Failed to close output file writer for file {}", file.getName(), e);
				}
			}

			result.setReport(report);
		} else {
			log.info("no file to process");
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	private void createAccountOperation(Subscription subscriptionInMemory, BigDecimal amountWithoutTax, String accountCode, int dueDateDelay, User currentUser)
			throws ImportInvoiceException {
		Subscription subscription = subscriptionService.findByCode(subscriptionInMemory.getCode(), currentUser.getProvider(),
				Arrays.asList("userAccount", "userAccount.billingAccount"));

		OCCTemplate invoiceTemplate = null;

		try {
			invoiceTemplate = oCCTemplateService.findByCode(paramBean.getProperty("accountOperationsGenerationJob.occCode", "FA_FACT"), currentUser.getProvider().getCode());
		} catch (Exception e) {
			log.error("occTemplate={}", e.getMessage());
			throw new ImportInvoiceException("Cannot find OCC Template with code=" + paramBean.getProperty("accountOperationsGenerationJob.occCode", "FA_FACT") + " for invoice");
		}

		if (invoiceTemplate == null) {
			throw new ImportInvoiceException("Cannot find OCC Template with code=" + paramBean.getProperty("accountOperationsGenerationJob.occCode", "FA_FACT") + " for invoice");
		}

		Date now = new Date();

		// recordedInvoice
		RecordedInvoice recordedInvoice = new RecordedInvoice();

		// recordedInvoice.setReference(invoice.getInvoiceNumber());
		recordedInvoice.setAccountCode(invoiceTemplate.getAccountCode());
		recordedInvoice.setOccCode(invoiceTemplate.getCode());
		recordedInvoice.setOccDescription(invoiceTemplate.getDescription());
		recordedInvoice.setTransactionCategory(invoiceTemplate.getOccCategory());
		recordedInvoice.setAccountCodeClientSide(invoiceTemplate.getAccountCodeClientSide());
		recordedInvoice.setMatchingAmount(BigDecimal.ZERO);
		recordedInvoice.setAmountWithoutTax(amountWithoutTax);
		recordedInvoice.setInvoiceDate(DateUtils.setTimeToZero(now));
		recordedInvoice.setTransactionDate(DateUtils.setTimeToZero(now));
		recordedInvoice.setMatchingStatus(MatchingStatusEnum.O);
		recordedInvoice.setBillingAccountName(subscription.getUserAccount().getBillingAccount().getCode());
		recordedInvoice.setCustomerAccount(subscription.getUserAccount().getBillingAccount().getCustomerAccount());
		recordedInvoice.setAccountCode(accountCode);

		try {
			recordedInvoice.setDueDate(DateUtils.setTimeToZero(DateUtils.addDaysToDate(now, dueDateDelay)));
		} catch (Exception e) {
			log.error("dueDate={}", e.getMessage());
			throw new ImportInvoiceException("Error on DueDate");
		}

		try {
			recordedInvoice.setPaymentMethod(subscription.getUserAccount().getBillingAccount().getPaymentMethod());
		} catch (IllegalStateException e) {
			log.warn("paymentMethod={}", e.getMessage());
		} catch (NullPointerException e) {
			log.warn("paymentMethod={}", e.getMessage());
		}

		BillingAccount billingAccount = subscription.getUserAccount().getBillingAccount();
		if (billingAccount.getBankCoordinates() != null) {
			recordedInvoice.setPaymentInfo(billingAccount.getBankCoordinates().getIban());
			recordedInvoice.setPaymentInfo1(billingAccount.getBankCoordinates().getBankCode());
			recordedInvoice.setPaymentInfo2(billingAccount.getBankCoordinates().getBranchCode());
			recordedInvoice.setPaymentInfo3(billingAccount.getBankCoordinates().getAccountNumber());
			recordedInvoice.setPaymentInfo4(billingAccount.getBankCoordinates().getKey());
			recordedInvoice.setPaymentInfo5(billingAccount.getBankCoordinates().getBankName());
			recordedInvoice.setPaymentInfo6(billingAccount.getBankCoordinates().getBic());
			recordedInvoice.setBillingAccountName(billingAccount.getBankCoordinates().getAccountOwner());
		}

		try {
			recordedInvoice.setTransactionCategory(OperationCategoryEnum.CREDIT);
		} catch (IllegalStateException e) {
			log.warn("rejectedType={}", e.getMessage());
		} catch (NullPointerException e) {
			log.warn("rejectedType={}", e.getMessage());
		}

		accountOperationService.create(recordedInvoice, currentUser, currentUser.getProvider());
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	private EDR rateEdr(WalletOperation walletOperation, String line, User currentUser, int dayOfMonth) throws ADRParsingException, BusinessException {
		EDR edr = aDRParsingService.getEDR(line, currentUser.getProvider(), dayOfMonth);
		if (edr != null) {
			log.debug("edr={}", edr);
			createEdr(edr, currentUser);

			try {
				WalletOperation tempWalletOperation = usageRatingService.rateUsageWithinNewTransaction(edr, currentUser);
				walletOperation.setAmountWithTax(tempWalletOperation.getAmountWithTax());
				if (edr.getStatus() == EDRStatusEnum.REJECTED) {
					log.error("edr rejected={}", edr.getRejectReason());
					throw new BusinessException(edr.getRejectReason());
				}
			} catch (BusinessException e) {
				log.error("Exception rating edr={}", e.getMessage());
				throw new BusinessException(e.getMessage());
			}
		}

		return edr;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void createEdr(EDR edr, User currentUser) throws BusinessException {
		edrService.create(edr, currentUser, currentUser.getProvider());
	}

	private void outputADR(String line) throws FileNotFoundException {
		if (outputFileWriter == null) {
			File outputFile = new File(outputDir + File.separator + adrFileName + ".processed");
			outputFileWriter = new PrintWriter(outputFile);
		}
		outputFileWriter.println(line);
	}

	private void rejectADR(Serializable adr, CDRRejectionCauseEnum reason) {

		if (rejectFileWriter == null) {
			File rejectFile = new File(rejectDir + File.separator + adrFileName + ".rejected");
			try {
				rejectFileWriter = new PrintWriter(rejectFile);
			} catch (FileNotFoundException e) {
				log.error("Failed to create a rejection file {}", rejectFile.getAbsolutePath());
			}
		}
		if (adr instanceof String) {
			rejectFileWriter.println(adr + "\t" + reason.name());
		} else {
			rejectFileWriter.println(adrParser.getCDRLine(adr, reason.name()));
		}

	}

	public Serializable getADR(String line, int dayOfMonth) throws InvalidAdrFormatException {
		ADR adr = new ADR();
		try {
			String[] fields = line.split(";");
			if (fields.length == 0) {
				throw new InvalidAdrFormatException(line, "record empty");
			} else if (fields.length < 7) {
				throw new InvalidAdrFormatException(line, "only " + fields.length + " in the record");
			} else {
				adr.setAccessId(fields[0]);
				if (StringUtils.isBlank(adr.getAccessId())) {
					throw new InvalidAccessException(line, "accessId is empty");
				}

				adr.setConsumptionMonth(new Integer(fields[1]));
				adr.setConsumptionYear(new Integer(fields[2]));
				adr.setConsumptionDay(dayOfMonth);
				adr.setTariffCode(fields[3]);
				adr.setQuantity(new BigDecimal(fields[4]));
				adr.setOid(fields[5]);
				adr.setCustomerCategory(fields[6]);
				if (fields.length == 8) {
					adr.setParameter1(fields[7]);
				}
			}
		} catch (Exception e) {
			throw new InvalidAdrFormatException(line, e.getMessage());
		}

		return adr;
	}

	class ADRParsingService {

		public EDR getEDR(String line, Provider provider, int dayOfMonth) throws ADRParsingException, BusinessException {
			Serializable adr = getADR(line, dayOfMonth);
			Access accessPoint = accessPointLookup(adr, provider);

			EDRDAO edrDAO = adrParser.getADR(adr);
			if ((accessPoint.getStartDate() == null || accessPoint.getStartDate().getTime() <= edrDAO.getEventDate().getTime())
					&& (accessPoint.getEndDate() == null || accessPoint.getEndDate().getTime() > edrDAO.getEventDate().getTime())) {
				EDR edr = new EDR();
				edr.setCreated(new Date());
				edr.setEventDate(edrDAO.getEventDate());
				edr.setOriginBatch(edrDAO.getOriginBatch());
				edr.setOriginRecord(edrDAO.getOriginRecord());
				edr.setParameter1(edrDAO.getParameter1());
				edr.setParameter2(edrDAO.getParameter2());
				edr.setParameter3(edrDAO.getParameter3());
				edr.setParameter4(edrDAO.getParameter4());
				edr.setProvider(accessPoint.getProvider());
				edr.setQuantity(edrDAO.getQuantity());
				edr.setStatus(EDRStatusEnum.OPEN);
				edr.setSubscription(accessPoint.getSubscription());

				return edr;
			} else {
				throw new InvalidAdrAccessException(adr);
			}
		}

		private Access accessPointLookup(Serializable adr, Provider provider) throws InvalidAdrAccessException, BusinessException {
			String accessUserId = adrParser.getAccessUserId(adr);
			List<Access> accesses = cdrEdrProcessingCacheContainerProvider.getAccessesByAccessUserId(provider.getId(), accessUserId);
			if (accesses == null || accesses.size() == 0) {
				((IProvider) adr).setProvider(provider);
				// TODO rejectededCdrEventProducer.fire(cdr);
				throw new InvalidAdrAccessException(adr);
			} else {
				// check for only 1 active subscription
				Access activeAccessSubscription = null;
				boolean hasActiveSubscription = false;
				for (Access access : accesses) {
					if (access.getSubscription().getStatus().equals(SubscriptionStatusEnum.ACTIVE)) {
						if (!hasActiveSubscription) {
							activeAccessSubscription = access;
							hasActiveSubscription = true;
						} else {
							throw new BusinessException("MULTIPLE_ACTIVE_SUBSCRIPTION");
						}
					}
				}

				if (!hasActiveSubscription) {
					throw new BusinessException("NO_ACTIVE_SUBSCRIPTION");
				}

				return activeAccessSubscription;
			}
		}

	}

	class ADRParser {

		private Logger log = LoggerFactory.getLogger(ADRParser.class);

		private String batchName;
		private String originBatch;
		private String username;
		private MessageDigest messageDigest = null;

		public void init(File adrFile) {
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				log.error("No message digest of type MD5", e);
			}

			batchName = "CDR_" + adrFile.getName();
		}

		public void initByApi(String username, String ip) {
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				log.error("No message digest of type MD5", e);
			}

			originBatch = "API_" + ip;
			this.username = username;
		}

		public String getAccessUserId(Serializable adr) throws InvalidAdrAccessException {
			String result = ((ADR) adr).getAccessId();
			if (result == null || result.trim().length() == 0) {
				throw new InvalidAdrAccessException(adr);
			}

			return result;
		}

		public String getOriginRecord(Serializable object) {
			String result = null;
			if (StringUtils.isBlank(username)) {
				ADR cdr = (ADR) object;
				result = cdr.toString();

				if (messageDigest != null) {
					synchronized (messageDigest) {
						messageDigest.reset();
						messageDigest.update(result.getBytes(Charset.forName("UTF8")));
						final byte[] resultByte = messageDigest.digest();
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < resultByte.length; ++i) {
							sb.append(Integer.toHexString((resultByte[i] & 0xFF) | 0x100).substring(1, 3));
						}
						result = sb.toString();
					}
				}
			} else {
				return username + "_" + new Date().getTime();
			}

			return result;
		}

		public EDRDAO getADR(Serializable object) {
			ADR adr = (ADR) object;
			EDRDAO result = new EDRDAO();
			result.setEventDate(adr.getEventDate());
			result.setOriginBatch(getOriginBatch());
			result.setOriginRecord(getOriginRecord(object));
			result.setQuantity(adr.getQuantity().setScale(BaseEntity.NB_DECIMALS, RoundingMode.HALF_UP));
			result.setParameter1(adr.getTariffCode());
			result.setParameter2(adr.getParameter1());

			return result;
		}

		public String getOriginBatch() {
			if (StringUtils.isBlank(originBatch)) {
				return batchName == null ? "CDR_CONS_CSV" : batchName;
			} else {
				return originBatch;
			}
		}

		public String getCDRLine(Serializable adr, String reason) {
			return ((ADR) adr).toString() + ";" + reason;
		}

	}

	class ADR implements Serializable, IProvider {

		private static final long serialVersionUID = 831068724267496530L;
		private String accessId;
		private int consumptionMonth;
		private int consumptionYear;
		private int consumptionDay;
		private String tariffCode;
		private BigDecimal quantity;
		private String oid;
		private String customerCategory;
		private String parameter1;
		private Provider provider;

		public Date getEventDate() {
			return DateUtils.newDate(consumptionYear, consumptionMonth, consumptionDay, 0, 0, 0);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			Formatter formatter = new Formatter(sb, Locale.US);

			String s = formatter.format("%s;%d;%d;%s;%f;%s;%s;%s;", accessId, consumptionMonth, consumptionYear, tariffCode, quantity, oid, customerCategory, parameter1)
					.toString();
			formatter.close();

			return s;

		}

		public String getAccessId() {
			return accessId;
		}

		public void setAccessId(String accessId) {
			this.accessId = accessId;
		}

		public int getConsumptionMonth() {
			return consumptionMonth;
		}

		public void setConsumptionMonth(int consumptionMonth) {
			this.consumptionMonth = consumptionMonth;
		}

		public int getConsumptionYear() {
			return consumptionYear;
		}

		public void setConsumptionYear(int consumptionYear) {
			this.consumptionYear = consumptionYear;
		}

		public String getTariffCode() {
			return tariffCode;
		}

		public void setTariffCode(String tariffCode) {
			this.tariffCode = tariffCode;
		}

		public BigDecimal getQuantity() {
			return quantity;
		}

		public void setQuantity(BigDecimal quantity) {
			this.quantity = quantity;
		}

		public String getOid() {
			return oid;
		}

		public void setOid(String oid) {
			this.oid = oid;
		}

		public String getCustomerCategory() {
			return customerCategory;
		}

		public void setCustomerCategory(String customerCategory) {
			this.customerCategory = customerCategory;
		}

		public String getParameter1() {
			return parameter1;
		}

		public void setParameter1(String parameter1) {
			this.parameter1 = parameter1;
		}

		public int getConsumptionDay() {
			return consumptionDay;
		}

		public void setConsumptionDay(int consumptionDay) {
			this.consumptionDay = consumptionDay;
		}

		@Override
		public Provider getProvider() {
			return provider;
		}

		@Override
		public void setProvider(Provider provider) {
			this.provider = provider;
		}

	}

}
