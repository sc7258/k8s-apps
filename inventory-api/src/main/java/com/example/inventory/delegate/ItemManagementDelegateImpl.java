package com.example.inventory.delegate;

import com.example.inventory.api.ItemManagementApiDelegate;
import com.example.inventory.entity.Item;
import com.example.inventory.model.ItemRequest;
import com.example.inventory.model.ItemResponse;
import com.example.inventory.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemManagementDelegateImpl implements ItemManagementApiDelegate {

    private final ItemService itemService;

    @Override
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemResponse> response = itemService.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ItemResponse> getItemById(Long id) {
        return ResponseEntity.ok(mapToResponse(itemService.findById(id)));
    }

    @Override
    public ResponseEntity<ItemResponse> createItem(ItemRequest itemRequest) {
        Item item = mapToEntity(itemRequest);
        Item savedItem = itemService.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedItem));
    }

    @Override
    public ResponseEntity<ItemResponse> updateItem(Long id, ItemRequest itemRequest) {
        Item itemDetails = mapToEntity(itemRequest);
        Item updatedItem = itemService.update(id, itemDetails);
        return ResponseEntity.ok(mapToResponse(updatedItem));
    }

    @Override
    public ResponseEntity<Void> deleteItem(Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ItemResponse mapToResponse(Item item) {
        ItemResponse response = new ItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setCategory(item.getCategory());
        response.setStatus(item.getStatus());
        response.setLocation(item.getLocation());
        if (item.getCreatedAt() != null) {
            response.setCreatedAt(item.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (item.getUpdatedAt() != null) {
            response.setUpdatedAt(item.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return response;
    }

    private Item mapToEntity(ItemRequest request) {
        return Item.builder()
                .name(request.getName())
                .category(request.getCategory())
                .status(request.getStatus())
                .location(request.getLocation())
                .build();
    }
}
