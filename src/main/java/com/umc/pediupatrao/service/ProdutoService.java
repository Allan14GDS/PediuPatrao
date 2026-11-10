package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public Produto novoProduto(Produto produto) {
        return produtoRepository.save(produto);
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }

    public Produto salvar(Produto produto) {
        if (produto.getId() == null) {
            return produtoRepository.save(produto);
        } else {
            if (produtoRepository.existsById(produto.getId())) {
                return produtoRepository.save(produto);
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }
}
