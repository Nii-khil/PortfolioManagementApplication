package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.jdbc.portfoliomanagement.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
public class HoldingService {
    @Autowired
    private HoldingRepository holdingRepository;

    public List<Holding> getAllHoldings() {
        return holdingRepository.findAll();
    }

    public Optional<Holding> getHoldingById(Long id) {
        return holdingRepository.findById(id);
    }

    public Holding createHolding(Holding holding) {
        return holdingRepository.save(holding);
    }

    public Optional<Holding> updateHolding(Long id, Holding holdingDetails) {
        return holdingRepository.findById(id)
                .map(holding -> {
                    holding.setAssetType(holdingDetails.getAssetType());
                    holding.setSymbol(holdingDetails.getSymbol());
                    holding.setQuantity(holdingDetails.getQuantity());
                    holding.setPurchasePrice(holdingDetails.getPurchasePrice());
                    holding.setPurchaseDate(holdingDetails.getPurchaseDate());
                    return holdingRepository.save(holding);
        });
    }

    public boolean deleteHolding(Long id) {
        if(holdingRepository.existsById(id)) {
            holdingRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Holding> getHoldingsByAssetType(String assetType) {
        return holdingRepository.findByAssetType(assetType);
    }


}
