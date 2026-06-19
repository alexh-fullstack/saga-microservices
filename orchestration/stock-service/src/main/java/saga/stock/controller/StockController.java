package saga.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import saga.stock.domain.InventoryEntity;
import saga.stock.domain.InventoryRepository;
import saga.stock.domain.StockReservationEntity;
import saga.stock.domain.StockReservationRepository;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @GetMapping("/inventory")
    public List<InventoryEntity> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @GetMapping("/reservations")
    public List<StockReservationEntity> getAllReservations() {
        return stockReservationRepository.findAll();
    }

    @PostMapping("/reset")
    public String resetStocks() {
        stockReservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        // Seed default inventory
        inventoryRepository.save(new InventoryEntity("JavaBook", 5));
        inventoryRepository.save(new InventoryEntity("RareVinyl", 0));
        return "Stock and inventory reset successfully";
    }
}
