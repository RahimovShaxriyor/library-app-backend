package order_service.order.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequestDto {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_SEASON_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_GENRE_LENGTH = 100;
    private static final int MAX_PUBLISHER_LENGTH = 100;
    private static final int MAX_ISBN_LENGTH = 20;

    @NotBlank(message = "Book name cannot be blank")
    @Size(min = 2, max = MAX_NAME_LENGTH, message = "Book name must be between 2 and {max} characters")
    private String name;

    @NotBlank(message = "Author cannot be blank")
    @Size(max = MAX_NAME_LENGTH, message = "Author name cannot exceed {max} characters")
    private String author;

    @NotBlank(message = "Season/part cannot be blank")
    @Size(max = MAX_SEASON_LENGTH, message = "Season cannot exceed {max} characters")
    private String season;

    @NotNull(message = "Number of pages cannot be null")
    @Positive(message = "Number of pages must be positive")
    @Max(value = 10000, message = "Number of pages cannot exceed {value}")
    private Integer pages;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be a positive value")
    @DecimalMin(value = "0.01", message = "Price must be at least {value}")
    @DecimalMax(value = "1000000.00", message = "Price cannot exceed {value}")
    private BigDecimal price;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 10000, message = "Quantity cannot exceed {value}")
    private Integer quantity;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Description cannot exceed {max} characters")
    private String description;

    @Size(max = MAX_GENRE_LENGTH, message = "Genre cannot exceed {max} characters")
    private String genre;

    @Size(max = MAX_PUBLISHER_LENGTH, message = "Publisher cannot exceed {max} characters")
    private String publisher;

    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
            message = "Invalid ISBN format")
    @Size(max = MAX_ISBN_LENGTH, message = "ISBN cannot exceed {max} characters")
    private String isbn;

    @Min(value = 1900, message = "Publication year must be after {value}")
    @Max(value = 2030, message = "Publication year cannot be after {value}")
    private Integer publicationYear;

    private String language;

    private Boolean availableForOrder;

    public String getTitle() {
        return this.name;
    }

    public void validate() {
        if (name != null && name.trim().isEmpty()) {
            throw new IllegalArgumentException("Book name cannot be empty");
        }

        if (author != null && author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be empty");
        }

        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        if (pages != null && pages <= 0) {
            throw new IllegalArgumentException("Pages must be positive");
        }

        validatePublicationYear();
        validateIsbn();
    }

    private void validatePublicationYear() {
        if (publicationYear != null && publicationYear > 2030) {
            throw new IllegalArgumentException("Publication year cannot be in the future");
        }
    }

    private void validateIsbn() {
        if (isbn != null && !isbn.trim().isEmpty()) {
            String cleanIsbn = isbn.replaceAll("[\\s-]", "");
            if (cleanIsbn.length() != 10 && cleanIsbn.length() != 13) {
                throw new IllegalArgumentException("ISBN must be 10 or 13 digits");
            }
        }
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasGenre() {
        return genre != null && !genre.trim().isEmpty();
    }

    public boolean hasPublisher() {
        return publisher != null && !publisher.trim().isEmpty();
    }

    public boolean hasIsbn() {
        return isbn != null && !isbn.trim().isEmpty();
    }

    public boolean isAvailableForOrder() {
        return availableForOrder != null ? availableForOrder : true;
    }

    public boolean isNewBook() {
        return publicationYear != null && publicationYear >= 2020;
    }

    public static BookRequestDto of(String name, String author, String season, Integer pages,
                                    BigDecimal price, Integer quantity) {
        return BookRequestDto.builder()
                .name(name)
                .author(author)
                .season(season)
                .pages(pages)
                .price(price)
                .quantity(quantity)
                .build();
    }

    public static BookRequestDto createWithDetails(String name, String author, String season,
                                                   Integer pages, BigDecimal price, Integer quantity,
                                                   String description, String genre, String publisher) {
        return BookRequestDto.builder()
                .name(name)
                .author(author)
                .season(season)
                .pages(pages)
                .price(price)
                .quantity(quantity)
                .description(description)
                .genre(genre)
                .publisher(publisher)
                .build();
    }

    public static BookRequestDto copyOf(BookRequestDto original) {
        return BookRequestDto.builder()
                .name(original.getName())
                .author(original.getAuthor())
                .season(original.getSeason())
                .pages(original.getPages())
                .price(original.getPrice())
                .quantity(original.getQuantity())
                .description(original.getDescription())
                .genre(original.getGenre())
                .publisher(original.getPublisher())
                .isbn(original.getIsbn())
                .publicationYear(original.getPublicationYear())
                .language(original.getLanguage())
                .availableForOrder(original.getAvailableForOrder())
                .build();
    }

    public void logDetails() {
        if (log.isDebugEnabled()) {
            log.debug("BookRequest - Name: {}, Author: {}, Pages: {}, Price: {}, Quantity: {}, " +
                            "HasDescription: {}, HasGenre: {}, HasPublisher: {}, HasISBN: {}",
                    name, author, pages, price, quantity,
                    hasDescription(), hasGenre(), hasPublisher(), hasIsbn());
        }
    }

    public BigDecimal calculateTotalValue() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean isHighValueBook() {
        return calculateTotalValue().compareTo(new BigDecimal("1000000")) > 0;
    }

    public boolean isLowStock() {
        return quantity != null && quantity <= 5;
    }

    @Override
    public String toString() {
        return String.format(
                "BookRequestDto{name='%s', author='%s', season='%s', pages=%d, price=%s, quantity=%d}",
                name, author, season, pages, price, quantity
        );
    }
}

