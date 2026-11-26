package com.kamuiprison.network;

import java.util.UUID;

/** Dados minimos de um prisioneiro enviados ao cliente para montar a interface de libertacao. */
public record PrisonerView(UUID id, String name, String typeName, boolean player) {
}
