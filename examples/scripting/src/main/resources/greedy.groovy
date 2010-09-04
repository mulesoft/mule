// Adapted from the Groovy Cookbook
// http://groovy.codehaus.org/Greedy+Coin+Changer+in+Groovy

enum USD {
    quarters(25), dimes(10), nickels(5), pennies(1)
    USD(v) { value = v }
    final value
}

enum GBP {
    two_pounds (200), pounds (100), fifty_pence(50), twenty_pence(20), ten_pence(10), five_pence(5), two_pence(2), pennies(1)
    GBP(v) { value = v }
    final value
}

def change(currency, amount) {
  currency.values().inject([]){ list, coin ->
     int count = amount / coin.value
     amount = amount % coin.value
     list += "$count $coin"
  }
}

switch (currency) {
    case "USD": return change(USD, payload).toString()
    case "GBP": return change(GBP, payload).toString()
    default: throw new AssertionError("Unsupported currency: $currency")
}
