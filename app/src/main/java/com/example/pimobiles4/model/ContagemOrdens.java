package com.example.pimobiles4.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ContagemOrdens {
    private final Map<StatusOrdemServico, Integer> porStatus = new EnumMap<>(StatusOrdemServico.class);
    private final int total;

    public ContagemOrdens(List<OrdemServico> ordens) {
        total = ordens.size();
        for (OrdemServico ordem : ordens) {
            StatusOrdemServico status = ordem.getStatus();
            if (status != null) porStatus.merge(status, 1, Integer::sum);
        }
    }

    public int total() {
        return total;
    }

    public int porStatus(StatusOrdemServico status) {
        return porStatus.getOrDefault(status, 0);
    }
}
