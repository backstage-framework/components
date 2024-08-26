/*
 *    Copyright 2019-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.backstage.app.api.controller;

import com.backstage.app.api.controller.request.CoffeeRequest;
import com.backstage.app.api.controller.request.NumberTypeRequest;
import com.backstage.app.exception.AppException;
import com.backstage.app.model.other.exception.ApiStatusCodeImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class SimpleTestController
{
	@GetMapping("/ping")
	public ResponseEntity<String> ping()
	{
		return ResponseEntity.ok("pong");
	}

	@PostMapping("/order")
	public ResponseEntity<?> order(@RequestBody CoffeeRequest request)
	{
		return ResponseEntity.ok(request);
	}

	@PostMapping("/calculate")
	public ResponseEntity<?> calculate(@RequestBody NumberTypeRequest request)
	{
		return ResponseEntity.ok(request);
	}

	@GetMapping("/exception")
	public ResponseEntity<?> exception() throws Exception
	{
		throw new Exception("Throwing test exception!", new UnsupportedOperationException("Empty method"));
	}

	@GetMapping("/appException")
	public ResponseEntity<?> appException() throws Exception
	{
		throw new AppException(ApiStatusCodeImpl.ILLEGAL_INPUT);
	}
}
