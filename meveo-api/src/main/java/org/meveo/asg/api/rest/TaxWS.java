package org.meveo.asg.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.api.ActionStatus;
import org.meveo.api.ActionStatusEnum;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.TaxDto;
import org.meveo.api.exception.CountryDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.TaxAlreadyExistsException;
import org.meveo.api.exception.TaxDoesNotExistsException;
import org.meveo.api.logging.LoggingInterceptor;
import org.meveo.asg.api.TaxServiceApi;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 9, 2013
 **/
@Path("/asg/tax")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Interceptors({ LoggingInterceptor.class })
public class TaxWS {

	@Inject
	private Logger log;

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private TaxServiceApi taxServiceApi;

	@GET
	@Path("/index")
	public ActionStatus index() {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS,
				"MEVEO API Rest Web Service");

		return result;
	}

	@POST
	@Path("/")
	public ActionStatus create(TaxDto taxDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		String taxId = taxDto.getTaxId();
		try {
			taxDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			taxDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			taxServiceApi.create(taxDto);
		} catch (CountryDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.COUNTRY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (TaxAlreadyExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.TAX_ALREADY_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.FAIL
				&& result.getErrorCode() != MeveoApiErrorCode.TAX_ALREADY_EXISTS) {
			taxServiceApi.removeAsgMapping(taxId, EntityCodeEnum.T);
		}
		
		if(result.getStatus() == ActionStatusEnum.FAIL) {
			log.error(result.getMessage());
		}

		return result;
	}

	@PUT
	@Path("/")
	public ActionStatus update(TaxDto taxDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			taxDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			taxDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			taxServiceApi.update(taxDto);
		} catch (TaxDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.TAX_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}
		
		if(result.getStatus() == ActionStatusEnum.FAIL) {
			log.error(result.getMessage());
		}

		return result;
	}

	@DELETE
	@Path("/{taxId}")
	public ActionStatus remove(@PathParam("taxId") String taxId) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			taxServiceApi.remove(taxId);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (TaxDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.TAX_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.SUCCESS) {
			taxServiceApi.removeAsgMapping(taxId, EntityCodeEnum.T);
		}
		
		if(result.getStatus() == ActionStatusEnum.FAIL) {
			log.error(result.getMessage());
		}

		return result;
	}

}
