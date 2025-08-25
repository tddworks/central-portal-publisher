# TDD School Choice Guide: Chicago vs London

A comprehensive guide to help you choose between Chicago School (Classicist) and London School (Mockist) TDD approaches based on your project, team, and context.

## Quick Decision Matrix

| Scenario | Recommended Approach | Why |
|----------|---------------------|-----|
| Core business logic & algorithms | Chicago School | Focus on state and outcomes |
| Integration with external systems | London School | Mock external dependencies |
| New team learning TDD | Chicago School | Simpler concepts, less tooling |
| Complex distributed systems | London School | Better isolation and contracts |
| Mathematical/computational logic | Chicago School | State-based verification natural |
| Event-driven architectures | London School | Interaction patterns important |
| Legacy system refactoring | Chicago School | Preserve existing behavior |
| Microservices boundaries | London School | Contract-first development |

## Core Philosophical Differences

### Chicago School (Classicist) Philosophy

**Core Belief:** *"Test the behavior, not the implementation"*

#### Fundamental Principles:
- **Empirical Testing**: Verify actual outcomes and results
- **Natural Object Collaboration**: Let objects work together as they would in production
- **Emergent Design**: Allow design to evolve from test requirements
- **Real World Simulation**: Use real objects to mimic production behavior
- **Minimal Intervention**: Mock only when absolutely necessary

#### Mental Model:
```
Test → Real System → Verify Outcome
 ↓
"What does this produce?"
```

#### Values:
- **Simplicity** over sophistication
- **Real behavior** over simulated behavior  
- **End results** over intermediate steps
- **Organic growth** over planned architecture

### London School (Mockist) Philosophy

**Core Belief:** *"Test the design through object interactions"*

#### Fundamental Principles:
- **Design by Contract**: Define interfaces through mock expectations
- **Isolated Testing**: Test units in complete isolation
- **Interaction-Driven**: Focus on how objects collaborate
- **Outside-In Thinking**: Start from user needs, work toward implementation
- **Explicit Dependencies**: Make all collaborations visible and testable

#### Mental Model:
```
Test → Mock Interactions → Verify Collaboration
 ↓
"How do objects talk to each other?"
```

#### Values:
- **Explicit design** over implicit emergence
- **Clear contracts** over emergent interfaces
- **Controlled isolation** over natural collaboration
- **Planned architecture** over organic growth

## Detailed Comparison

### Development Flow

#### Chicago School Flow:
1. Write failing test for desired outcome
2. Implement minimal code to pass test
3. Refactor while keeping tests green
4. Let design emerge from accumulated tests
5. Use real objects throughout the flow

#### London School Flow:
1. Design object interactions through mocks
2. Write test with mock expectations
3. Implement to satisfy mock contracts
4. Verify interactions work as designed
5. Build from outside interfaces inward

### Testing Strategy

#### Chicago School Testing:
```kotlin
// Focus on what the system produces
@Test
fun `should process payment and update account balance`() {
    val account = Account(initialBalance = 100.0)
    val paymentProcessor = PaymentProcessor(realBankGateway)
    
    val result = paymentProcessor.processPayment(account, 50.0)
    
    // Verify the final state
    assertThat(result.isSuccessful).isTrue()
    assertThat(account.balance).isEqualTo(50.0)
    assertThat(account.transactions).hasSize(1)
}
```

#### London School Testing:
```kotlin
// Focus on how objects collaborate
@Test
fun `should coordinate payment processing workflow`() {
    val mockBankGateway = mock<BankGateway>()
    val mockNotificationService = mock<NotificationService>()
    val paymentProcessor = PaymentProcessor(mockBankGateway, mockNotificationService)
    
    whenever(mockBankGateway.charge(any(), any())).thenReturn(ChargeResult.Success)
    
    paymentProcessor.processPayment(account, 50.0)
    
    // Verify the interactions occurred
    verify(mockBankGateway).charge(account.id, 50.0)
    verify(mockNotificationService).sendPaymentConfirmation(account.email)
}
```

## Self-Assessment Questions

### Choose Chicago School if you answer "Yes" to most:

**Team & Experience:**
- [ ] Is your team new to TDD?
- [ ] Do you prefer simple tools and minimal setup?
- [ ] Does your team value emergent design over upfront planning?
- [ ] Are you comfortable with design evolving during development?

**Project Characteristics:**
- [ ] Are you building core business logic or algorithms?
- [ ] Is the domain well-understood with clear rules?
- [ ] Do you have objects with obvious state that can be verified?
- [ ] Is the system primarily computational or data-transformational?

**Technical Context:**
- [ ] Are external dependencies minimal or easily stubbed?
- [ ] Can you create simple in-memory implementations for testing?
- [ ] Is performance of test execution important?
- [ ] Do you prefer tests that survive refactoring?

### Choose London School if you answer "Yes" to most:

**Team & Experience:**
- [ ] Does your team have strong OOP design experience?
- [ ] Are you comfortable with mocking frameworks and tools?
- [ ] Do you prefer to design interfaces upfront?
- [ ] Is your team disciplined about maintaining mock contracts?

**Project Characteristics:**
- [ ] Are you building integration layers or API boundaries?
- [ ] Does your system have complex object interactions?
- [ ] Are you working with external services or systems?
- [ ] Is the domain complex with unclear or evolving requirements?

**Technical Context:**
- [ ] Do you need to isolate units from expensive dependencies?
- [ ] Are you building microservices with clear boundaries?
- [ ] Do you need to verify specific interaction patterns?
- [ ] Is contract-first development important for your architecture?

## Contextual Recommendations

### By Project Type

#### Web Applications
- **API Layer**: London School (mock external services)
- **Business Logic**: Chicago School (verify domain behavior)
- **Database Layer**: Chicago School (use in-memory databases)
- **Integration Tests**: Mixed approach

#### Domain-Rich Applications
- **Core Domain**: Chicago School (focus on business rules)
- **Application Services**: London School (coordinate between layers)
- **Infrastructure**: London School (mock external systems)

#### Data Processing Systems
- **Algorithms**: Chicago School (verify transformations)
- **Pipeline Orchestration**: London School (coordinate stages)
- **Data Validation**: Chicago School (verify rules)

### By Team Maturity

#### Beginner TDD Teams
**Recommendation: Start with Chicago School**
- Easier to understand and implement
- Less tooling and setup required
- More forgiving of mistakes
- Natural progression to testing mindset

#### Advanced TDD Teams
**Recommendation: Use both approaches strategically**
- Chicago School for core logic
- London School for integration boundaries
- Context-driven decisions
- Tool expertise enables either approach

### By System Architecture

#### Monolithic Applications
- **Recommendation: Primarily Chicago School**
- Objects can collaborate naturally
- Fewer integration boundaries
- Simpler dependency management

#### Microservices Architecture
- **Recommendation: Primarily London School**
- Clear service boundaries
- Contract-first development
- Explicit service interactions

#### Event-Driven Systems
- **Recommendation: London School**
- Focus on message patterns
- Verify event handling
- Mock event publishers/subscribers

## Hybrid Approach Strategy

### Layer-Based Selection
```
┌─────────────────────────────────────┐
│  UI/API Layer (London School)       │  ← Mock business services
├─────────────────────────────────────┤
│  Business Logic (Chicago School)    │  ← Test domain behavior
├─────────────────────────────────────┤
│  Infrastructure (London School)     │  ← Mock external systems
└─────────────────────────────────────┘
```

### Practical Implementation
```kotlin
// Domain Layer - Chicago School
class OrderCalculatorTest {
    @Test
    fun `should calculate total with discounts and tax`() {
        val calculator = OrderCalculator()
        val order = Order(items = listOf(
            Item("Book", 20.00),
            Item("Pen", 5.00)
        ))
        
        val result = calculator.calculate(order, discountRate = 0.1, taxRate = 0.08)
        
        assertThat(result.subtotal).isEqualTo(25.00)
        assertThat(result.discount).isEqualTo(2.50)
        assertThat(result.tax).isEqualTo(2.43)
        assertThat(result.total).isEqualTo(24.93)
    }
}

// Application Layer - London School  
class OrderServiceTest {
    @Test
    fun `should process order through complete workflow`() {
        val mockInventory = mock<InventoryService>()
        val mockPayment = mock<PaymentService>()
        val mockNotification = mock<NotificationService>()
        
        val orderService = OrderService(mockInventory, mockPayment, mockNotification)
        
        whenever(mockInventory.reserveItems(any())).thenReturn(ReservationResult.Success)
        whenever(mockPayment.processPayment(any())).thenReturn(PaymentResult.Success)
        
        orderService.processOrder(order)
        
        verify(mockInventory).reserveItems(order.items)
        verify(mockPayment).processPayment(order.total)
        verify(mockNotification).sendOrderConfirmation(order.customerId)
    }
}
```

## Migration Strategies

### From Chicago to London
1. **Identify Integration Points**: Find boundaries between layers
2. **Introduce Mocks Gradually**: Start at system boundaries
3. **Preserve Core Logic Tests**: Keep Chicago School for domain
4. **Add Contract Tests**: Verify mock interactions

### From London to Chicago
1. **Reduce Mock Usage**: Replace mocks with real implementations
2. **Focus on Outcomes**: Change assertions from interactions to state
3. **Simplify Test Setup**: Remove complex mock configurations
4. **Preserve Integration Mocks**: Keep mocks for external systems

## Common Pitfalls and Solutions

### Chicago School Pitfalls
**Problem**: Tests become slow due to real object usage
**Solution**: Use lightweight in-memory implementations

**Problem**: Tests break easily during refactoring
**Solution**: Focus on essential behavior, not internal structure

**Problem**: Difficulty testing error conditions
**Solution**: Use test doubles for error-prone dependencies

### London School Pitfalls
**Problem**: Tests become brittle and implementation-coupled
**Solution**: Mock at service boundaries, not every dependency

**Problem**: Over-mocking leads to false positives
**Solution**: Balance mocks with integration tests

**Problem**: Mock maintenance becomes expensive
**Solution**: Use contract tests to verify mock accuracy

## Final Recommendation Framework

### Start Here:
1. **Assess your context** using the self-assessment questions
2. **Begin with Chicago School** if you're new to TDD
3. **Introduce London School** at integration boundaries
4. **Evolve your approach** based on experience and feedback

### Remember:
- **Both approaches are valid** - choose based on context
- **Hybrid approaches work well** - use both strategically  
- **Team preference matters** - consistency is important
- **Context drives decisions** - no one-size-fits-all solution

The goal is not to choose perfectly, but to choose consciously and adapt as you learn.