package com.cursed.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.dtos.RegisterClientDto;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.entities.OAuthClient;
import com.cursed.auth.services.OAuthClientService;
import com.cursed.auth.utils.CommonUtils;

@RestController
@RequestMapping("/api/client")
public class ClientController {
    private final OAuthClientService oAuthClientService;

    public ClientController(OAuthClientService oAuthClientService) {
        this.oAuthClientService = oAuthClientService;
    }

    @GetMapping("{clientId}")
    public ResponseEntity<BaseResponseDTO<OAuthClient>> getClientById(
            @PathVariable(name = "clientId") String clientId) {
        return CommonUtils.handleResponse(oAuthClientService.getClientByClientId(clientId));
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDTO<OAuthClient>> createClient(@RequestBody RegisterClientDto client) {
        return CommonUtils.handleResponse(oAuthClientService.createClient(client));
    }

    @PutMapping("{clientId}")
    public ResponseEntity<BaseResponseDTO<OAuthClient>> updateClient(
            @PathVariable(name = "clientId") String clientId,
            @RequestBody RegisterClientDto client) {
        return CommonUtils.handleResponse(oAuthClientService.updateClient(clientId, client));
    }

    @DeleteMapping("{clientId}")
    public ResponseEntity<BaseResponseDTO<OAuthClient>> deleteClient(
            @PathVariable(name = "clientId") String clientId) {
        return CommonUtils.handleResponse(oAuthClientService.deleteClient(clientId));
    }
}
