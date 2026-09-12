package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Items in MongoDB.
 */
@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

    /**
     * Items reported by one user.
     *
     * @param ownerId the reporter
     * @return that user's items
     */
    List<Item> findByOwnerId(String ownerId);

    /**
     * Items in one category.
     *
     * @param category the category
     * @return matching items
     */
    List<Item> findByCategory(String category);

    /**
     * Items of one type ("Lost" or "Found").
     *
     * @param type Lost or Found
     * @return matching items
     */
    List<Item> findByType(String type);

    /**
     * Items with one status.
     *
     * @param status OPEN or CLOSED
     * @return matching items
     */
    List<Item> findByStatus(String status);

    /**
     * Found reports written for one of these lost reports.
     */
    List<Item> findByTargetLostReportIdIn(List<String> targetLostReportIds);

    /**
     * Found reports one user filed for a specific lost report.
     */
    List<Item> findByOwnerIdAndTargetLostReportId(String ownerId, String targetLostReportId);

    /**
     * One user's reports of a given type and status.
     */
    List<Item> findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(
            String ownerId, String type, String status);

    /**
     * Open found reports in one category, reported at or after a lost date.
     */
    List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategoryAndReportedAtGreaterThanEqual(
            String type, String status, String category, Instant reportedAt);

    /**
     * Open found reports in one category when the lost date is missing.
     */
    List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategory(
            String type, String status, String category);
}
