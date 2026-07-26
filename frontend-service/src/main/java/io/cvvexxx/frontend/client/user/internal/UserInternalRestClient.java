package io.cvvexxx.frontend.client.user.internal;

import io.cvvexxx.frontend.dto.ProductOwnerDto;

import java.util.List;
import java.util.UUID;

public interface UserInternalRestClient {

    List<ProductOwnerDto> findAllUsersByIds(List<UUID> userIds);

    ProductOwnerDto findUserById(UUID userId);
}
