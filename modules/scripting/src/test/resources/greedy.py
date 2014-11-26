# Adapted from "Python Algorithms: Greedy Coin Changer" by Noah Gift
# http://www.oreillynet.com/onlamp/blog/2008/04/python_greedy_coin_changer_alg.html

import sys

class Change:
    def __init__(self, currency, amount):
        self.amount = amount
        if currency == "USD":
            self.coins = [1,5,10,25]
            self.coin_lookup = {25: "quarters", 10: "dimes", 5: "nickels", 1: "pennies"}
        elif currency == "GBP":
            self.coins = [1,2,5,10,20,50,100,200]
            self.coin_lookup = {200: "two_pounds", 100: "pounds", 50: "fifty_pence", 20: "twenty_pence", 10: "ten_pence", 5: "five_pence", 2: "two_pence", 1: "pennies"}
        #else:
        #	print "Currency $currency not recognized"
        #	exit 1
        self.result = ""

    def printer(self,num,coin):
        if num:
            if coin in self.coin_lookup:
                if self.result == "":
                    self.result = '%1.0f %s' % (num, self.coin_lookup[coin])
                else:
                    self.result = '%s, %1.0f %s' % (self.result, num, self.coin_lookup[coin])

    def recursive_change(self, rem):
        if len(self.coins) == 0:
            return []
        coin = self.coins.pop()
        num, new_rem = divmod(rem, coin)
        self.printer(num,coin)
        return self.recursive_change(new_rem) + [num]

c = Change(currency, payload)
c.recursive_change(c.amount)
result = "[" + c.result + "]"
