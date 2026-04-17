@cart
Feature: Cart API — Pharmacy Cart Operations

  Background:
    * url baseUrl
    * def patientId = 1

  @smoke
  Scenario: Retrieve active cart items for a patient
    Given path '/todos'
    And param userId = patientId
    When method GET
    Then status 200
    And match response == '#[] { userId: #number, id: #number, title: #string, completed: #boolean }'

  @smoke
  Scenario: Add an item to the patient cart
    Given path '/todos'
    And request { userId: 1, title: 'Metformin 500mg — Qty: 60', completed: false }
    When method POST
    Then status 201
    And match response.title == 'Metformin 500mg — Qty: 60'
    And match response.completed == false

  Scenario: Mark a cart item as fulfilled (checkout)
    Given path '/todos/1'
    And request { userId: 1, id: 1, title: 'Existing prescription item', completed: true }
    When method PUT
    Then status 200
    And match response.completed == true

  Scenario: Remove an item from the cart
    Given path '/todos/1'
    When method DELETE
    Then status 200
