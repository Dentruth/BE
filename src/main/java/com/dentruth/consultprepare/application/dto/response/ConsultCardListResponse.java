package com.dentruth.consultprepare.application.dto.response;

import com.dentruth.consultprepare.application.dto.request.ConsultCardListItemResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConsultCardListResponse {

    private List<ConsultCardListItemResponse> result;
}
