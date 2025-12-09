package com.example.paymentservice.repository;

import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.model.PaymentProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Basic find methods
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findByProviderTransactionId(String providerTransactionId);

    List<Payment> findAllByOrderId(Long orderId, Pageable pageable);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByProviderAndStatus(PaymentProvider provider, PaymentStatus status);

    List<Payment> findByCreatedAtBeforeAndStatus(LocalDateTime dateTime, PaymentStatus status);

    List<Payment> findByNextRetryAtBeforeAndStatus(LocalDateTime dateTime, PaymentStatus status);

    Page<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Count methods
    Long countByStatus(PaymentStatus status);

    Long countByProvider(PaymentProvider provider);

    Long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.createdAt >= :dateTime AND p.status = :status")
    Long countByCreatedAtAfterAndStatus(@Param("dateTime") LocalDateTime dateTime,
                                        @Param("status") PaymentStatus status);

    // Sum methods for analytics
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.createdAt >= :dateTime AND p.status = 'SUCCESS'")
    Optional<BigDecimal> sumAmountByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.createdAt BETWEEN :start AND :end AND p.status = 'SUCCESS'")
    Optional<BigDecimal> sumAmountByCreatedAtBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    // Grouping for statistics - ИСПРАВЛЕННЫЕ МЕТОДЫ
    @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
    List<Object[]> countByStatusGroup();

    @Query("SELECT p.provider, COUNT(p) FROM Payment p GROUP BY p.provider")
    List<Object[]> countByProviderGroup();

    // Maintenance queries
    @Query("SELECT p FROM Payment p WHERE p.expiresAt < :now AND p.status = 'PENDING'")
    List<Payment> findExpiredPayments(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Payment p WHERE p.nextRetryAt < :now AND p.status = 'FAILED' AND p.attemptCount < p.maxAttempts")
    List<Payment> findPaymentsReadyForRetry(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Payment p WHERE p.providerTransactionId IS NULL AND p.status = 'PENDING' AND p.createdAt < :threshold")
    List<Payment> findAbandonedPayments(@Param("threshold") LocalDateTime threshold);

    // Analytics and reporting
    @Query(value = """
        SELECT EXTRACT(HOUR FROM created_at) as hour, 
               COUNT(*) as count,
               COALESCE(SUM(amount), 0) as volume
        FROM payments 
        WHERE created_at >= :startDate 
          AND status = 'SUCCESS'
        GROUP BY EXTRACT(HOUR FROM created_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<Map<String, Object>> getHourlyStats(@Param("startDate") LocalDateTime startDate);



    @Query("SELECT p FROM Payment p WHERE p.metadata LIKE %:idempotencyKey%")
    Optional<Payment> findByMetadataContainingIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    // Refund related queries
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCESS' AND p.refundAmount IS NULL")
    List<Payment> findRefundablePayments();

    @Query("SELECT p FROM Payment p WHERE p.status = 'REFUNDED' AND p.refundedAt >= :since")
    List<Payment> findRefundedPaymentsSince(@Param("since") LocalDateTime since);

    // Customer related queries
    @Query("SELECT p FROM Payment p WHERE p.customerPhone = :phoneNumber ORDER BY p.createdAt DESC")
    List<Payment> findByCustomerPhone(@Param("phoneNumber") String phoneNumber, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.customerEmail = :email ORDER BY p.createdAt DESC")
    List<Payment> findByCustomerEmail(@Param("email") String email, Pageable pageable);

    // Merchant specific queries
    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId AND p.createdAt BETWEEN :start AND :end")
    Page<Payment> findByMerchantIdAndPeriod(@Param("merchantId") String merchantId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            Pageable pageable);

    // Bulk operations for maintenance
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'CANCELLED', p.cancellationReason = 'System cleanup', p.cancelledAt = :now " +
            "WHERE p.expiresAt < :now AND p.status = 'PENDING'")
    int cancelExpiredPayments(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Payment p SET p.nextRetryAt = :nextRetry, p.attemptCount = p.attemptCount + 1 " +
            "WHERE p.id IN :paymentIds")
    int updateRetryInfo(@Param("paymentIds") List<Long> paymentIds,
                        @Param("nextRetry") LocalDateTime nextRetry);

    // Advanced analytics
    @Query(value = """
        SELECT 
            provider,
            status,
            COUNT(*) as transaction_count,
            COALESCE(SUM(amount), 0) as total_amount,
            AVG(amount) as average_amount,
            MIN(created_at) as first_transaction,
            MAX(created_at) as last_transaction
        FROM payments 
        WHERE created_at BETWEEN :start AND :end
        GROUP BY provider, status
        ORDER BY provider, status
        """, nativeQuery = true)
    List<Map<String, Object>> getProviderAnalytics(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            TO_CHAR(created_at, 'YYYY-MM-DD') as date,
            COUNT(*) as daily_count,
            COALESCE(SUM(amount), 0) as daily_volume,
            COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) as success_count,
            COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count
        FROM payments 
        WHERE created_at BETWEEN :start AND :end
        GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD')
        ORDER BY date
        """, nativeQuery = true)
    List<Map<String, Object>> getDailyStats(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);


    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, p.createdAt, p.completedAt)) FROM Payment p " +
            "WHERE p.status = 'SUCCESS' AND p.completedAt IS NOT NULL AND p.createdAt >= :since")
    Optional<Double> getAverageProcessingTime(@Param("since") LocalDateTime since);


    @Query("SELECT p FROM Payment p WHERE p.amount > :threshold AND p.status = 'PENDING'")
    List<Payment> findHighValuePendingPayments(@Param("threshold") BigDecimal threshold);

    @Query("SELECT p.customerPhone, COUNT(p) as paymentCount " +
            "FROM Payment p WHERE p.createdAt >= :since AND p.status = 'SUCCESS' " +
            "GROUP BY p.customerPhone HAVING COUNT(p) > :threshold")
    List<Map<String, Object>> findFrequentCustomers(@Param("since") LocalDateTime since,
                                                    @Param("threshold") Long threshold);

    // Audit and compliance
    @Query("SELECT p FROM Payment p WHERE p.updatedAt >= :since AND (p.status != 'PENDING' OR p.providerTransactionId IS NOT NULL)")
    List<Payment> findRecentlyModifiedPayments(@Param("since") LocalDateTime since);

    // Health check queries
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.createdAt >= :recentTime")
    Long countRecentPayments(@Param("recentTime") LocalDateTime recentTime);

    @Query("SELECT MAX(p.createdAt) FROM Payment p")
    Optional<LocalDateTime> getLastPaymentTime();

    // Currency specific queries
    @Query("SELECT p FROM Payment p WHERE p.currency = :currency AND p.status = 'SUCCESS'")
    List<Payment> findSuccessfulPaymentsByCurrency(@Param("currency") String currency);

    @Query("SELECT p.currency, COUNT(p), COALESCE(SUM(p.amount), 0) " +
            "FROM Payment p WHERE p.status = 'SUCCESS' GROUP BY p.currency")
    List<Object[]> getCurrencyStats();

    // Batch processing support
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :cutoffTime")
    List<Payment> findOldPaymentsByStatus(@Param("status") PaymentStatus status,
                                          @Param("cutoffTime") LocalDateTime cutoffTime,
                                          Pageable pageable);
}