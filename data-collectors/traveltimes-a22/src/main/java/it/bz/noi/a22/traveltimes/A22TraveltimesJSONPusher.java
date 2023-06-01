// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22.traveltimes;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;

@Component
public class A22TraveltimesJSONPusher extends NonBlockingJSONPusher
{

	private String stationtype;
	private String origin;

	@Autowired
	private Environment env;

	public <T> DataMapDto<RecordDtoImpl> mapData(T arg0)
	{
		throw new IllegalStateException("it is used by who?");
	}

	@Override
	@PostConstruct
	public void init() {
		A22Properties prop = new A22Properties("a22traveltimes.properties");
		stationtype = prop.getProperty("stationtype");
		origin = prop.getProperty("origin");
		super.init();
	}

	@Override
	public String initIntegreenTypology()
	{
		return stationtype;
	}

	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null, env.getProperty("provenance_name"), env.getProperty("provenance_version"),  origin);
	}
}
