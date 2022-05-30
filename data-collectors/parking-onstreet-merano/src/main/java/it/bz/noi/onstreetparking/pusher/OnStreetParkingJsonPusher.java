package it.bz.noi.onstreetparking.pusher;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;
import it.bz.noi.onstreetparking.configuration.OnStreetParkingConfiguration;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class OnStreetParkingJsonPusher extends NonBlockingJSONPusher {

	private static final Logger LOG = LoggerFactory.getLogger(OnStreetParkingJsonPusher.class);

	protected String origin;

	@Autowired
	protected Environment env;
	@Autowired
	protected OnStreetParkingConfiguration onStreetParkingConfiguration;

	public <T> DataMapDto<RecordDtoImpl> mapData(T arg0) {
		throw new IllegalStateException("it is used by who?");
	}

	@PostConstruct
	public void init() {
		LOG.info("start init");
		origin = onStreetParkingConfiguration.getOrigin();
		super.init();
		LOG.info("end init");
	}

	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null, env.getProperty("provenance_name"), env.getProperty("provenance_version"), origin);
	}

	@Override
	public String initIntegreenTypology() {
		return onStreetParkingConfiguration.getStationtype();
	}
}
