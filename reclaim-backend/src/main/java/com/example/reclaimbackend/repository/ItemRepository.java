package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link Item} documents.
 */
@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

    /**
     * Finds all items belonging to a specific user.
     *
     * @param ownerId the ID of the item owner
     * @return list of items reported by the user
     */
    List<Item> findByOwnerId(String ownerId);

    /**
     * Finds all items matching a given category.
     *
     * @param category the category to filter by
     * @return list of items in the specified category
     */
    List<Item> findByCategory(String category);

    /**
     * Finds all items matching a given type ("Lost" or "Found").
     *
     * @param type the type to filter by
     * @return list of items of the specified type
     */
    List<Item> findByType(String type);

    /**
     * Finds all items matching a given status.
     *
     * @param status the status to filter by (e.g. "OPEN", "CLOSED")
     * @return list of items with the specified status
     */
    List<Item> findByStatus(String status);
}
