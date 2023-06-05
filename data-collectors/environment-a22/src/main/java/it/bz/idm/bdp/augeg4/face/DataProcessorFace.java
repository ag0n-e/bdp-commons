// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4.face;

import it.bz.idm.bdp.augeg4.dto.AugeG4ProcessedData;
import it.bz.idm.bdp.augeg4.dto.AugeG4RawData;

import java.util.List;

public interface DataProcessorFace {
    List<AugeG4ProcessedData> process(List<AugeG4RawData> data);
}
