// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcbikesharingpapin;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;

@Lazy
@Service
public class BikesharingPapinDataPusher extends NonBlockingJSONPusher {

    private static final Logger LOG = LoggerFactory.getLogger(BikesharingPapinDataPusher.class.getName());

    @Autowired
    private Environment env;

    public BikesharingPapinDataPusher() {
        LOG.debug("START.constructor.");
        LOG.debug("END.constructor.");
    }

    @Override
    public String initIntegreenTypology() {
        String stationType = BikesharingPapinDataConverter.STATION_TYPE_STATION;
        return stationType;
    }


    @Override
    public String toString() {
        String str1 = "http://" + config.getString(HOST_KEY) + ":" + config.getString(PORT_KEY) + config.getString("json_endpoint");
        String str2 =
                "integreenTypology=" + this.integreenTypology   + "  " +
                        "DEFAULT_HOST="      + DEFAULT_HOST     + "  " +
                        "DEFAULT_PORT="      + DEFAULT_PORT     + "  " +
                        "DEFAULT_ENDPOINT="  + DEFAULT_ENDPOINT + "  " +
                        "";
        return str2 + " ---> " + str1;
    }

    @Override
    public ProvenanceDto defineProvenance() {
        return new ProvenanceDto(null,env.getProperty("provenance_name"), env.getProperty("provenance_version"),  env.getProperty("app_origin"));
    }

    @Override
    public <T> DataMapDto<RecordDtoImpl> mapData(T data) {
        // TODO Auto-generated method stub
        return null;
    }
}
