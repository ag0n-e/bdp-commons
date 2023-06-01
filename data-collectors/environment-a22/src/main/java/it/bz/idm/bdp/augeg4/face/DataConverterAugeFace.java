// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4.face;

import it.bz.idm.bdp.augeg4.dto.AugeG4ProcessedData;
import it.bz.idm.bdp.augeg4.dto.toauge.AugeG4ProcessedDataToAugeDto;

import java.util.List;

public interface DataConverterAugeFace {

    List<AugeG4ProcessedDataToAugeDto> convert(List<AugeG4ProcessedData> processedData);
}
