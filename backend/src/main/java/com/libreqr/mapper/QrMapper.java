package com.libreqr.mapper;

import com.libreqr.document.QrCode;
import com.libreqr.dto.QrResponse;
import org.springframework.stereotype.Component;

/**
 * Conversion QrCode (documento) -> QrResponse (DTO).
 */
@Component
public class QrMapper {

    public QrResponse toResponse(QrCode qr) {
        return new QrResponse(
                qr.getId(),
                qr.getType(),
                qr.getStatus(),
                qr.getName(),
                qr.getDescription(),
                qr.getSlug(),
                qr.getPayload(),
                qr.getCampaignId(),
                qr.getDesignConfig(),
                qr.getImageUrl(),
                qr.getType() == com.libreqr.document.QrType.url, // solo los dinamicos son editables
                qr.getCreatedAt(),
                qr.getUpdatedAt());
    }
}
