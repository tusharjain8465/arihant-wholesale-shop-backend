package com.example.wholesalesalesbackend.mapper;

import com.example.wholesalesalesbackend.dto.ClientDTO;
import com.example.wholesalesalesbackend.model.Client;

public class SaleEntryMapper {

    public static ClientDTO toClientDTO(Client client) {
        if (client == null)
            return null;

        ClientDTO.ClientDTOBuilder builder = ClientDTO.builder();

        if (client.getId() != null)
            builder.id(client.getId());

        if (client.getName() != null)
            builder.name(client.getName());

        if (client.getLocation() != null)
            builder.location(client.getLocation());

        if (client.getContact() != null)
            builder.contact(client.getContact());

        return builder.build();
    }

    

}
