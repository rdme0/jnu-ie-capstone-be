package jnu.ie.capstone.common.converter

import jnu.ie.capstone.common.constant.enums.SortField
import jnu.ie.capstone.common.dto.request.CustomPageRequest
import jnu.ie.capstone.common.exception.client.InvalidPageableFieldException
import org.springframework.core.convert.converter.Converter
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class CustomPageRequestToPageableConverter(
    private val allowedFields: List<SortField>
) : Converter<CustomPageRequest, Pageable> {
    override fun convert(request: CustomPageRequest): Pageable {
        val page = request.page?.minus(1) ?: 0
        val size = request.size ?: 10
        val directionString = request.direction ?: "asc"
        val requestFieldString = request.sort ?: SortField.CREATED_AT.requestField

        val direction: Sort.Direction = try {
            Sort.Direction.fromString(directionString.uppercase())
        } catch (_: IllegalArgumentException) {
            throw InvalidPageableFieldException("direction", directionString)
        }

        val sortField: SortField = allowedFields.find { it.requestField == requestFieldString }
            ?: throw InvalidPageableFieldException("sort", requestFieldString)
        val order = Sort.Order(direction, sortField.dbField)
        return PageRequest.of(page, size, Sort.by(order))
    }
}