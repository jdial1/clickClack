# Bundled font

`app/src/main/res/font/lilita_one.ttf` is **Lilita One**, from Google Fonts,
under the **SIL Open Font License 1.1**.

The OFL requires the licence text to travel with the font. The canonical copy
lives at <https://github.com/google/fonts/blob/main/ofl/lilitaone/OFL.txt> and
could not be fetched from this machine -- the network here rejects
raw.githubusercontent.com on certificate grounds -- so **`OFL-LilitaOne.txt`
still has to be dropped in beside this file before any public release.**

The Kenney art (bricks, sounds, emotes) is CC0 and needs no notice; only the
files the app uses are in `app/src/main/res`.

# Personality statements

`app/src/main/assets/daily/daily-ipip-*.json` hold questions written around the traits
in the
**International Personality Item Pool** IPIP-NEO facet scales
(<https://ipip.ori.org/>), which are **public domain**: the site states they may
be copied, edited, translated or used for any purpose without permission or fee.
No notice is required; the credit is kept anyway, in `Sources.IPIP`.

Goldberg, L. R., Johnson, J. A., Eber, H. W., Hogan, R., Ashton, M. C.,
Cloninger, C. R., & Gough, H. G. (2006). The International Personality Item Pool
and the future of public-domain personality measures. *Journal of Research in
Personality, 40*(1), 84-96.

Regenerate with `python tools/ipip/ipip_questions.py`.

The other `daily-ipip-*` decks (strengths, feelings, hexaco, work, pastimes)
come from the same pool: IPIP-VIA, Barchard's (2001) emotional-intelligence
components, IPIP-HEXACO (Ashton, Lee & Goldberg, 2007), and the Oregon
Vocational (Pozzebon et al., 2010) and Avocational (Goldberg, 2010) Interest
Scales. All public domain.

# Written for the app

`daily-us-*.json` (funny, romantic, spicy, deep) are original. Regenerate with
`python tools/questions/couples.py`.

# Adapted couples questions

- `daily-album.json`: the confession-album questions answered by Marcel Proust
  (c. 1886, 1891) and Karl Marx (1865). Public domain by age; only the original
  album questions were used, not later magazine versions.
- `daily-onesource.json`: after Military OneSource, *What to Ask Your Partner
  Before Marriage* (US Department of Defense). Public domain.
- `daily-azura.json`: selected and reworded from
  <https://github.com/OperationAzura/questionServer>, under this notice:

  > MIT License. Copyright (c) 2023 OperationAzura.
  >
  > Permission is hereby granted, free of charge, to any person obtaining a copy
  > of this software and associated documentation files (the "Software"), to deal
  > in the Software without restriction, including without limitation the rights
  > to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  > copies of the Software, and to permit persons to whom the Software is
  > furnished to do so, subject to the following conditions:
  >
  > The above copyright notice and this permission notice shall be included in all
  > copies or substantial portions of the Software.
  >
  > THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  > IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  > FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  > AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  > LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  > OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  > SOFTWARE.

# Government sources

- `daily-atus-*.json`: activities from the **American Time Use Survey Activity
  Lexicon** (US Bureau of Labor Statistics, 2024), reworded. Public domain.
- `daily-cfpb-money.json`: after the **CFPB Financial Well-Being Scale**
  (Consumer Financial Protection Bureau). Public domain.

# Attribution required

`daily-couple-week.json` adapts the Relational Behavior Interactions scale from
Harel, T., & Koslowsky, M. (2024). Development and validation of the relational
behavior interactions scale for couples. *Scientific Reports, 14*, 8086.
<https://doi.org/10.1038/s41598-024-58901-2>. Licensed **CC BY 4.0**
(<https://creativecommons.org/licenses/by/4.0/>). Changes: items reworded to
address your partner and turned into daily questions.

CC BY needs this credit to be visible to people using the app, not only in the
repo. **Before any public release, show `Sources` on a screen in the app.**

Regenerate these with `python tools/questions/build_open_sources.py`.
