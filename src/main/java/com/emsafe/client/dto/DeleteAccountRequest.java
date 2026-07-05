package com.emsafe.client.dto;

/** Confirmation payload for self-service account deletion (current password). */
public record DeleteAccountRequest(String password) {}
