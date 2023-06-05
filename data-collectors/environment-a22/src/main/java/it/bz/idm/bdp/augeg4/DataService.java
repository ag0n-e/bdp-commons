// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4;

import it.bz.idm.bdp.augeg4.face.DataPusherAugeFace;
import it.bz.idm.bdp.augeg4.face.DataPusherHubFace;
import it.bz.idm.bdp.augeg4.face.DataRetrieverFace;
import it.bz.idm.bdp.augeg4.face.DataServiceFace;

public class DataService implements DataServiceFace {

    private final DataRetrieverFace dataRetriever;
    private final DataPusherHubFace dataPusherHub;
    private final DataPusherAugeFace dataPusherAuge;

    private final StationsDelegate stationsDelegate = new StationsDelegate(this);
    private final DataTypeDelegate dataTypeDelegate = new DataTypeDelegate(this);
    private final DataDelegate dataDelegate = new DataDelegate(this);

    public DataService(
            DataRetrieverFace dataRetriever,
            DataPusherHubFace dataPusherHub,
            DataPusherAugeFace dataPusherAuge
    ) {
        this.dataRetriever = dataRetriever;
        this.dataPusherHub = dataPusherHub;
        this.dataPusherAuge = dataPusherAuge;
    }

    @Override
    public void loadPreviouslySyncedStations() throws Exception {
        stationsDelegate.loadPreviouslySyncedStations();
    }

    /**
     * Loads from HUB previously synced stations and then sends stations in the stationsMap to the HUB
     */
    @Override
    public void syncStations() {
        stationsDelegate.syncStationsWithHub();
    }

    /**
     * Sends to HUB the list of known DataTypes
     */
    @Override
    public void syncDataTypes() {
        dataTypeDelegate.syncDataTypes();
    }

    /**
     * Dequeues converted data and sends it to the HUB
     */
    @Override
    public void pushData() {
        dataDelegate.pushData();
    }

    DataPusherHubFace getDataPusherHub() {
        return dataPusherHub;
    }

    DataRetrieverFace getDataRetriever() {
        return dataRetriever;
    }

    DataPusherAugeFace getDataPusherAuge() {
        return dataPusherAuge;
    }

    StationsDelegate getStationsDelegate() {
        return stationsDelegate;
    }

    public int getStationsCount() {
        return stationsDelegate.getStationsCount();
    }

    public int getDataToAugeCount() {
        return dataDelegate.getDataToAugeCount();
    }

    public int getDataToHubCount() {
        return dataDelegate.getDataToHubCount();
    }
}
