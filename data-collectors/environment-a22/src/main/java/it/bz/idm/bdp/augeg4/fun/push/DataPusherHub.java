package it.bz.idm.bdp.augeg4.fun.push;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import it.bz.idm.bdp.augeg4.dto.tohub.AugeG4ProcessedDataToHubDto;
import it.bz.idm.bdp.augeg4.face.DataPusherHubFace;
import it.bz.idm.bdp.augeg4.face.DataPusherMapperFace;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;

@Service
public class DataPusherHub extends NonBlockingJSONPusher implements DataPusherHubFace {


    @Value("${station.type}")
    private  String stationType;
    @Value("${origin}")
    private String stationOrigin;

    private static final Logger LOG = LoggerFactory.getLogger(DataPusherHub.class.getName());

    private final DataPusherMapperFace mapper;

    private DataMapDto<RecordDtoImpl> rootMap;

    @Autowired
    private Environment env;

    public DataPusherHub(@Value("${period}") int period) {
        mapper = new DataPusherMapper(period);
    }

    @Override
    public Object syncDataTypes(List<DataTypeDto> dataTypeDtoList) {
        List<DataTypeDto> mappedDataTypeDtoList = mapper.mapDataTypes(dataTypeDtoList);
        return super.syncDataTypes(mappedDataTypeDtoList);
    }

    @Override
    public void pushData() {
        LOG.info("PUSH DATA");
        if (rootMap.getBranch().keySet().isEmpty()) {
            LOG.warn("pushData() rootMap.getBranch().keySet().isEmpty()");
        } else {
            // FIXME integreenTypology substitute with real value
            LOG.debug("PUSH Typology: ["+this.integreenTypology+"]");
            super.pushData(this.integreenTypology, rootMap);
        }
    }

    /**
     * TODO: Define your station type, which must be present in bdp-core/dal and derived from "Station"
     */
    @Override
    public String initIntegreenTypology() {
        return getStationType();
    }

    @Override
    public String getStationType() {
        return stationType;
    }

    @Override
    public String getOrigin() {
        return stationOrigin;
    }

    @Override
    public <T> DataMapDto<RecordDtoImpl> mapData(T data) {
        try {
            @SuppressWarnings("unchecked")
            List<AugeG4ProcessedDataToHubDto> measurementsByStation = ((List<AugeG4ProcessedDataToHubDto>) data);
            return rootMap = mapper.mapData(measurementsByStation);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException("data must be of type List<AugeG4ProcessedDataToHubDto>", ex);
        }
    }

    @Override
    public StationList getSyncedStations() {
        List<StationDto> stations = super.fetchStations(getStationType(), getOrigin());
        return new StationList(stations);
    }

    protected DataMapDto<RecordDtoImpl>  getRootMap() {
        return this.rootMap;
    }

	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null, env.getProperty("provenance_name"), env.getProperty("provenance_version"),  env.getProperty("origin"));
	}
}
