// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4.dto.tohub;

public class ProcessedMeasurementToHub {

    private final String dataType;

    private final double rawValue;

    private final Double processedValue;

    public ProcessedMeasurementToHub(String dataType, double rawValue, Double processedValue) {
        this.dataType = dataType;
        this.rawValue = rawValue;
        this.processedValue = processedValue;
    }

    public String getDataType() {
        return dataType;
    }

    public double getRawValue() {
        return rawValue;
    }

    public Double getProcessedValue() {
        return processedValue;
    }

    @Override
    public String toString() {
        return "ProcessedMeasurementToHub{" +
                "dataType='" + dataType + '\'' +
                ", rawValue=" + rawValue +
                ", processedValue=" + processedValue +
                '}';
    }
}
