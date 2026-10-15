package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Cliente;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClienteRepository extends MongoRepository<Cliente, String> {
    List<Cliente> findByTelefone(String telefone);
}
