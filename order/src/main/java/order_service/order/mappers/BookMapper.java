package order_service.order.mappers;

import order_service.order.dto.BookRequestDto;
import order_service.order.dto.BookResponseDto;
import order_service.order.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {

    BookResponseDto toDto(Book book);
    Book toEntity(BookRequestDto bookRequestDto);
    void updateEntityFromDto(BookRequestDto dto, @MappingTarget Book book);
}