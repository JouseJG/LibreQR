package com.libreqr.dto;

import java.util.UUID;

/** Escaneos humanos del mes en curso atribuidos a una campana. */
public record CampaignScanStat(UUID campaignId, String name, long scans) {
}
