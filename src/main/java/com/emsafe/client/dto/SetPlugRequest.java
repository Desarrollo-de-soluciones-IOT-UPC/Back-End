package com.emsafe.client.dto;

/** Orden del cliente para el relé de su dispositivo: "ON" (dar corriente) | "OFF" (cortar). */
public record SetPlugRequest(String plug) {}
