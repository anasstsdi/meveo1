package org.meveo.api.ws.impl;

import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.jws.WebService;

import org.meveo.api.CountryIsoApi;
import org.meveo.api.CurrencyIsoApi;
import org.meveo.api.LanguageIsoApi;
import org.meveo.api.ProviderApi;
import org.meveo.api.SuperAdminPermission;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.CountryIsoDto;
import org.meveo.api.dto.CurrencyIsoDto;
import org.meveo.api.dto.LanguageIsoDto;
import org.meveo.api.dto.ProviderDto;
import org.meveo.api.dto.response.GetCountryIsoResponse;
import org.meveo.api.dto.response.GetCurrencyIsoResponse;
import org.meveo.api.dto.response.GetLanguageIsoResponse;
import org.meveo.api.dto.response.GetProviderResponse;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.ws.SuperAdminSettingsWs;

/**
 * @author Edward P. Legaspi
 **/
@WebService(serviceName = "SuperAdminSettingsWs", endpointInterface = "org.meveo.api.ws.SuperAdminSettingsWs",targetNamespace = "http://superAdmin.ws.api.meveo.org/")
@Interceptors({ WsRestApiInterceptor.class })
@SuperAdminPermission
public class SuperAdminSettingsWsImpl extends BaseWs implements SuperAdminSettingsWs {

	@Inject
	private CountryIsoApi countryIsoApi;

	@Inject
	private LanguageIsoApi languageIsoApi;

	@Inject
	private CurrencyIsoApi currencyIsoApi;

	@Inject
	private ProviderApi providerApi;

	@Override
	public ActionStatus createProvider(ProviderDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			providerApi.create(postData, getCurrentUser());

		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public GetProviderResponse findProvider(String providerCode) {
		GetProviderResponse result = new GetProviderResponse();

		try {
			result.setProvider(providerApi.find(providerCode, getCurrentUser()));

		} catch (Exception e) {
			super.processException(e, result.getActionStatus());
		}

		return result;
	}

	@Override
	public ActionStatus updateProvider(ProviderDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			providerApi.update(postData, getCurrentUser(postData.getCode()));
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createOrUpdateProvider(ProviderDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			providerApi.createOrUpdate(postData, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createLanguage(LanguageIsoDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			languageIsoApi.create(postData, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public GetLanguageIsoResponse findLanguage(String languageCode) {
		GetLanguageIsoResponse result = new GetLanguageIsoResponse();

		try {
			result.setLanguage(languageIsoApi.find(languageCode));
		} catch (Exception e) {
			super.processException(e, result.getActionStatus());
		}
		return result;
	}

	@Override
	public ActionStatus removeLanguage(String languageCode) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			languageIsoApi.remove(languageCode, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus updateLanguage(LanguageIsoDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			languageIsoApi.update(postData, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}
		return result;
	}

	@Override
	public ActionStatus createOrUpdateLanguage(LanguageIsoDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			languageIsoApi.createOrUpdate(postData, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createCountry(CountryIsoDto countryIsoDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			countryIsoApi.create(countryIsoDto, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public GetCountryIsoResponse findCountry(String countryCode) {
		GetCountryIsoResponse result = new GetCountryIsoResponse();

		try {
			result.setCountry(countryIsoApi.find(countryCode));
		} catch (Exception e) {
			super.processException(e, result.getActionStatus());
		}

		return result;
	}

	@Override
	public ActionStatus removeCountry(String countryCode) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			countryIsoApi.remove(countryCode, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus updateCountry(CountryIsoDto countryIsoDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			countryIsoApi.update(countryIsoDto, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createOrUpdateCountry(CountryIsoDto countryIsoDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			countryIsoApi.createOrUpdate(countryIsoDto, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createCurrency(CurrencyIsoDto currencyIsoDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			currencyIsoApi.create(currencyIsoDto, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public GetCurrencyIsoResponse findCurrency(String currencyCode) {
		GetCurrencyIsoResponse result = new GetCurrencyIsoResponse();

		try {
			result.setCurrency(currencyIsoApi.find(currencyCode));
		} catch (Exception e) {
			super.processException(e, result.getActionStatus());
		}

		return result;
	}

	@Override
	public ActionStatus removeCurrency(String currencyCode) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			currencyIsoApi.remove(currencyCode, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus updateCurrency(CurrencyIsoDto currencyIsoDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			currencyIsoApi.update(currencyIsoDto, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}

	@Override
	public ActionStatus createOrUpdateCurrency(CurrencyIsoDto currencyIsoDto) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			currencyIsoApi.createOrUpdate(currencyIsoDto, getCurrentUser());
		} catch (Exception e) {
			super.processException(e, result);
		}

		return result;
	}
}
