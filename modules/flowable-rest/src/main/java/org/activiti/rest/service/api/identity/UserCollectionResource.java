/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.rest.service.api.identity;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.IdentityService;
import org.activiti.engine.query.QueryProperty;
import org.activiti.idm.api.User;
import org.activiti.idm.api.UserQuery;
import org.activiti.idm.api.UserQueryProperty;
import org.activiti.rest.api.DataResponse;
import org.activiti.rest.exception.ActivitiConflictException;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Users" }, description = "Manage Users")
public class UserCollectionResource {

  protected static HashMap<String, QueryProperty> properties = new HashMap<String, QueryProperty>();

  static {
    properties.put("id", UserQueryProperty.USER_ID);
    properties.put("firstName", UserQueryProperty.FIRST_NAME);
    properties.put("lastName", UserQueryProperty.LAST_NAME);
    properties.put("email", UserQueryProperty.EMAIL);
  }

  @Autowired
  protected RestResponseFactory restResponseFactory;

  @Autowired
  protected IdentityService identityService;

  @ApiOperation(value = "Get a list of users", tags = {"Users"})
  @ApiImplicitParams({
          @ApiImplicitParam(name = "id", dataType = "string", value = "Only return group with the given id", paramType = "query"),
          @ApiImplicitParam(name = "firstName", dataType = "string", value = "Only return users with the given firstname", paramType = "query"),
          @ApiImplicitParam(name = "lastName", dataType = "string", value = "Only return users with the given lastname", paramType = "query"),
          @ApiImplicitParam(name = "email", dataType = "string", value = "Only return users with the given email", paramType = "query"),
          @ApiImplicitParam(name = "firstNameLike", dataType = "string", value = "Only return userswith a firstname like the given value. Use % as wildcard-character.", paramType = "query"),
          @ApiImplicitParam(name = "lastNameLike", dataType = "string", value = "Only return users with a lastname like the given value. Use % as wildcard-character.", paramType = "query"),
          @ApiImplicitParam(name = "emailLike", dataType = "string", value = "Only return users with an email like the given value. Use % as wildcard-character.", paramType = "query"),
          @ApiImplicitParam(name = "memberOfGroup", dataType = "string", value = "Only return users which are a member of the given group.", paramType = "query"),
          @ApiImplicitParam(name = "potentialStarter", dataType = "string", value = "Only return users  which members are potential starters for a process-definition with the given id.", paramType = "query"),
          @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues ="id,firstName,lastname,email", paramType = "query"),
  })
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Indicates the group exists and is returned.")
  })
  @RequestMapping(value = "/identity/users", method = RequestMethod.GET, produces = "application/json")
  public DataResponse getUsers(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
    UserQuery query = identityService.createUserQuery();

    if (allRequestParams.containsKey("id")) {
      query.userId(allRequestParams.get("id"));
    }
    if (allRequestParams.containsKey("firstName")) {
      query.userFirstName(allRequestParams.get("firstName"));
    }
    if (allRequestParams.containsKey("lastName")) {
      query.userLastName(allRequestParams.get("lastName"));
    }
    if (allRequestParams.containsKey("email")) {
      query.userEmail(allRequestParams.get("email"));
    }
    if (allRequestParams.containsKey("firstNameLike")) {
      query.userFirstNameLike(allRequestParams.get("firstNameLike"));
    }
    if (allRequestParams.containsKey("lastNameLike")) {
      query.userLastNameLike(allRequestParams.get("lastNameLike"));
    }
    if (allRequestParams.containsKey("emailLike")) {
      query.userEmailLike(allRequestParams.get("emailLike"));
    }
    if (allRequestParams.containsKey("memberOfGroup")) {
      query.memberOfGroup(allRequestParams.get("memberOfGroup"));
    }

    return new UserPaginateList(restResponseFactory).paginateList(allRequestParams, query, "id", properties);
  }

  @ApiOperation(value = "Create a user", tags = {"Users"})
  @ApiResponses(value = {
          @ApiResponse(code = 201, message = "Indicates the user was created."),
          @ApiResponse(code = 400, message = "Indicates the id of the user was missing.")

  })
  @RequestMapping(value = "/identity/users", method = RequestMethod.POST, produces = "application/json")
  public UserResponse createUser(@RequestBody UserRequest userRequest, HttpServletRequest request, HttpServletResponse response) {
    if (userRequest.getId() == null) {
      throw new ActivitiIllegalArgumentException("Id cannot be null.");
    }

    // Check if a user with the given ID already exists so we return a
    // CONFLICT
    if (identityService.createUserQuery().userId(userRequest.getId()).count() > 0) {
      throw new ActivitiConflictException("A user with id '" + userRequest.getId() + "' already exists.");
    }

    User created = identityService.newUser(userRequest.getId());
    created.setEmail(userRequest.getEmail());
    created.setFirstName(userRequest.getFirstName());
    created.setLastName(userRequest.getLastName());
    created.setPassword(userRequest.getPassword());
    identityService.saveUser(created);

    response.setStatus(HttpStatus.CREATED.value());

    return restResponseFactory.createUserResponse(created, true);
  }

}
