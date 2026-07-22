package io.cvvexxx.frontend.client.user.internal;

import io.cvvexxx.frontend.dto.ProductOwnerDto;

import java.util.List;

public interface UserInternalRestClient {

    List<ProductOwnerDto> findAllUsersByIds(List<Integer> userIds);

    ProductOwnerDto findUserById(Integer userId);
}
