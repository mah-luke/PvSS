## Online resources
- https://www.boardgamebeast.com/risk-board-game-strategies-south-america.html
- https://www.tutorials.chymera.eu/blog/2014/09/18/1v1-risk-strategy/
- https://www.tutorials.chymera.eu/blog/2014/07/23/per-attack-risk-dice-odds/
- https://www4.stat.ncsu.edu/~jaosborn/research/RISK.pdf

## Dice Odds
| Attacker | Defender | Event            | Ratio     | Probability |
|---------:|---------:|:-----------------|:----------|:------------|
|        1 |        1 | Defender loses 1 | 15/36     | 0.417       |
|        1 |        1 | Attacker loses 1 | 21/36     | 0.583       |
|        1 |        2 | Defender loses 1 | 55/216    | 0.255       |
|        1 |        2 | Attacker loses 1 | 161/216   | 0.745       |
|        2 |        1 | Defender loses 1 | 125/216   | 0.579       |
|        2 |        1 | Attacker loses 1 | 91/216    | 0.421       |
|        2 |        2 | Defender loses 2 | 295/1296  | 0.228       |
|        2 |        2 | Each lose 1      | 420/1296  | 0.324       |
|        2 |        2 | Attacker loses 2 | 581/1296  | 0.448       |
|        3 |        1 | Defender loses 1 | 855/1296  | 0.660       |
|        3 |        1 | Attacker loses 1 | 441/1296  | 0.340       |
|        3 |        2 | Defender loses 2 | 2890/7776 | 0.372       |
|        3 |        2 | Each lose 1      | 2611/7776 | 0.336       |
|        3 |        2 | Attacker loses 2 | 2275/7776 | 0.293       |

## Troop loss

| Attacker | Defender | Loss |
|---------:|---------:|:-----|
|        1 |        1 | 1.40 |
|        1 |        2 | 2.93 |
|        2 |        1 | 0.73 |
|        2 |        2 | 1.14 |
|        3 |        1 | 0.52 |
|        3 |        2 | 0.95 |
Troops lost by the attacker per 1 unit of defender.

## Fight Outcome
| A\D | 1     | 2     | 3     | 4     | 5     | 6     | 7     | 8     | 9     | 10    |
|----:|:------|:------|:------|:------|:------|:------|:------|:------|:------|:------|
|   1 | 0.417 | 0.106 | 0.027 | 0.007 | 0.002 | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 |
|   2 | 0.754 | 0.363 | 0.206 | 0.091 | 0.049 | 0.021 | 0.011 | 0.005 | 0.003 | 0.001 |
|   3 | 0.916 | 0.656 | 0.470 | 0.315 | 0.206 | 0.134 | 0.084 | 0.054 | 0.033 | 0.021 |
|   4 | 0.972 | 0.785 | 0.642 | 0.477 | 0.359 | 0.253 | 0.181 | 0.123 | 0.086 | 0.057 |
|   5 | 0.990 | 0.890 | 0.769 | 0.638 | 0.506 | 0.397 | 0.297 | 0.224 | 0.162 | 0.118 |
|   6 | 0.997 | 0.934 | 0.857 | 0.745 | 0.638 | 0.521 | 0.423 | 0.329 | 0.258 | 0.193 |
|   7 | 0.999 | 0.967 | 0.910 | 0.834 | 0.736 | 0.640 | 0.536 | 0.446 | 0.357 | 0.287 |
|   8 | 1.000 | 0.980 | 0.947 | 0.888 | 0.818 | 0.730 | 0.643 | 0.547 | 0.464 | 0.380 |
|   9 | 1.000 | 0.990 | 0.967 | 0.930 | 0.873 | 0.808 | 0.726 | 0.646 | 0.558 | 0.480 |
|  10 | 1.000 | 0.994 | 0.981 | 0.954 | 0.916 | 0.861 | 0.800 | 0.724 | 0.650 | 0.568 |
Probability that the attacker wins

## Heuristics

### Set-Up Phase (beginning of game)
- [x] Try to go for South America, then North America
- [x] Prevent continent bonus for enemy
- [ ] Central America most important?
- [ ] fine-tune priority of territories

### Reinforcement Phase
- [ ] Trade cards as early/late as possible?
- [ ] Reinforce troops at borders
- [ ] Avoid reinforcing territories containing less than 3 troops
- [ ] Reinforce regions connected to multiple enemy territories

### Attack Phase
- [ ] Always attack if one has >=3 attackers, until troops left is close to 3.
- [ ] Best attacks are 3v1 and 2v1.
- [ ] Avoid attacking territories next to large stacks.

### Occupy Phase
- [ ] Move with how many troops?

### Fortify Phase
- [ ] Move troops to conflict territories



## Game Plan

### Strategy
#### Setup Phase
- Focus on South America, then go for North America.
- Prevent any continent bonus in the first round.

#### Main Phase
- Conquer South America, then North America.
- Fortify Alaska heavily to prevent any breakthrough?


### Tactics
- Always reinforce borders
- Never reinforce territory with only friendly neighbours.

## Summary
- PDF One-page description of the implemented methods, ideas, references, etc.
- Name of the game engine
- student contact data
- any data that is necessary to identify the project as yours