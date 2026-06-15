/* Logistics Calculator
   Tests: Const initialization, parameter scopes, float/int promotion,
   complex boolean logic, and iterative statements.
*/

const float vatRate = 0.15;
const int baseCharge = 50;

float calculateFreight(float distance, int weight) {
    float totalCost = 0.0;
    const float longHaulMultiplier = 1.5;

    // Test complex relational and logical operators
    if (distance > 100.0 && weight >= 50) {
        // Ints and floats interacting (widening coercion test)
        totalCost = (distance * longHaulMultiplier) + (weight * 2.5);
    } else {
        totalCost = baseCharge + (weight * 1.5);
    }

    // Apply global constant
    totalCost = totalCost + (totalCost * vatRate);

    return totalCost;
}

int main() {
    int shipmentID = 1045;
    float abebeDistance = 150.5;
    int packageWeight = 60;

    float finalReceipt;
    // Test function call and assignment
    finalReceipt = calculateFreight(abebeDistance, packageWeight);

    int i;
    int routingNodes = 0;

    // Test standard iterative block
    for(i = 0; i < 3; i = i + 1) {
        routingNodes = routingNodes + 1;
    }

    return 0;
}