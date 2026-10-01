package com.example.pimobiles4.data;
import com.example.pimobiles4.model.OrdemServico;
import java.util.List;
public interface OrdemRepository {
    List<OrdemServico> listar();
    OrdemServico buscar(long id);
    void salvar(OrdemServico ordem);
    void excluir(long id);
}
