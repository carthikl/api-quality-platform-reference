@cart @smoke
Feature: Cart API — Pharmacy Cart Operations

  Background:
    * url baseUrl

  Scenario: Get cart items for valid patient returns collection
    Given path '/todos'
    And param userId = 1
    When method GET
    Then status 200
    And match response == '#[]'
    And assert response.length > 0
    And match each response == { id: '#number', userId: '#number', title: '#string', completed: '#boolean' }

  Scenario: Add item to cart returns 201 with ID
    Given path '/todos'
    And request { userId: 1, title: 'Metformin 500mg - Qty: 60', completed: false }
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.title == 'Metformin 500mg - Qty: 60'
    And match response.completed == false

  Scenario: Cart response content-type is JSON
    Given path '/todos'
    When method GET
    Then status 200
    And match responseHeaders['Content-Type'][0] contains 'application/json'
