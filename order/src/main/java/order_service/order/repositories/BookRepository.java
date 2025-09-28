package order_service.order.repositories;


import order_service.order.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = "SELECT * FROM book WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Book> findByIdForUpdate(@Param("id") Long id);
}