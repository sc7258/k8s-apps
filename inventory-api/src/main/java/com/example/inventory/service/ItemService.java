package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));
    }

    @Transactional
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        itemRepository.deleteById(id);
    }

    @Transactional
    public Item update(Long id, Item itemDetails) {
        Item item = findById(id);
        item.setName(itemDetails.getName());
        item.setCategory(itemDetails.getCategory());
        item.setStatus(itemDetails.getStatus());
        item.setLocation(itemDetails.getLocation());
        return item;
    }
}
