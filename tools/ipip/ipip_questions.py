"""
Turn the IPIP-NEO 300 facet items (public domain, ipip.ori.org) into daily questions.

Every facet group yields one question of each of the eight types, so the types
come out evenly spread. Statements become first person ("Worry about things" ->
"I worry about things"). Items that would read as an accusation in a couples
game, clinical items, and the politics/religion facet are left out.

"Me" and "I" in a question mean whoever is answering; the partner guesses.
"""
import io, json, os, re
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "daily") + os.sep

facets = {f["id"]: f for f in json.load(open(os.path.join(HERE, "ipip_neo300.json")))}

# Left out: clinical, accusatory, or political. A couples app that asks you to
# guess whether your partner "takes advantage of others" is not a game.
EXCLUDE = {
    "Dislike myself", "Have a low opinion of myself", "Feel desperate",
    "Feel that my life lacks direction", "Go on binges", "Feel that I'm unable to deal with things",
    "Am not really interested in others",
    "Look down on others", "Am indifferent to the feelings of others", "Make people feel uncomfortable",
    "Turn my back on others",
    "Yell at people", "Insult people", "Get back at others",
    "Boast about my virtues", "Believe that I am better than others",
    "Use flattery to get ahead", "Use others for my own ends", "Cheat to get ahead",
    "Put people under pressure", "Pretend to be concerned for others", "Take advantage of others",
    "Obstruct others' plans",
    "Sympathize with the homeless", "Tend to dislike soft-hearted people", "Believe in an eye for an eye",
    "Try not to think about the needy", "Believe people should fend for themselves", "Can't stand weak people",
    "Don't understand things", "Have little to contribute",
    "Misrepresent the facts", "Break my promises",
}

# group key, facets merged into it, category, tags, depth for the list types,
# a guess-me prompt, a reflect prompt, reflect depth.
GROUPS = [
    ("n1", ["N1"], "Deep", ["words"], 2,
     "What is the thing I worry about most often, even when I do not bring it up?",
     "What helps you stop worrying once you have started?", 2),
    ("n2", ["N2"], "Deep", ["words"], 2,
     "What is the small, everyday thing most likely to get under my skin?",
     "When you are irritated, what do you want the other person to do?", 2),
    ("n3", ["N3"], "Deep", ["words", "touch"], 2,
     "What do I do when I am feeling low and do not want to say so?",
     "What is something that reliably lifts you out of a gray mood?", 3),
    ("n4", ["N4"], "Know me", ["friends", "words"], 1,
     "What kind of situation makes me feel most out of place?",
     "When did you last feel shy, and what got you past it?", 2),
    ("n5", ["N5"], "Food", ["fun", "money"], 1,
     "What is the one thing I can never resist, however full I am?",
     "What is a small indulgence you would never give up?", 1),
    ("n6", ["N6"], "Deep", ["acts", "words"], 2,
     "When everything goes wrong at once, what is the first thing I do?",
     "What is the hardest thing you have gotten through, and what got you through it?", 3),
    ("e1", ["E1"], "Know me", ["friends"], 1,
     "How long does it take me to feel comfortable with someone new?",
     "Who is the easiest person you have ever gotten to know, and why?", 1),
    ("e2", ["E2"], "Playful", ["friends", "fun"], 1,
     "What is my ideal number of people at a party, not counting us?",
     "What is the best gathering you have ever been part of?", 1),
    ("e3", ["E3"], "Know me", ["work", "words"], 1,
     "In a group trying to decide where to eat, what role do I end up playing?",
     "When do you wish you had spoken up sooner?", 2),
    ("e4", ["E4"], "Everyday", ["time"], 1,
     "What does my perfect lazy day look like, hour by hour?",
     "Does your week feel too full, too empty, or about right, and why?", 2),
    ("e5", ["E5"], "Playful", ["fun", "planning"], 1,
     "What is the most daring thing I would actually do if you dared me?",
     "What is the most adventurous thing you have ever done, and would you do it again?", 1),
    ("e6", ["E6"], "Playful", ["fun"], 1,
     "What never fails to make me laugh?",
     "When did you last laugh until it hurt?", 1),
    ("o1", ["O1"], "Know me", ["words"], 1,
     "What do I daydream about when I am staring out the window?",
     "What is something you imagined as a kid that you still think about?", 2),
    ("o2", ["O2"], "Know me", ["fun", "time"], 1,
     "What song, painting, or place would I call beautiful?",
     "What is something beautiful you noticed this week that nobody else seemed to?", 1),
    ("o3", ["O3"], "Deep", ["words"], 2,
     "What is something that moves me more than I let on?",
     "When did you last cry at something, and what was it?", 2),
    ("o4", ["O4"], "Travel", ["planning", "fun"], 1,
     "What is one routine I would never give up, even on vacation?",
     "What is something new you want us to try together this year?", 1),
    ("o5", ["O5"], "Know me", ["words"], 1,
     "What topic could I talk about for an hour without noticing the time?",
     "What is an idea that changed how you see things?", 2),
    ("a1", ["A1"], "Deep", ["friends", "words"], 2,
     "How long does it take me to really trust someone?",
     "Who taught you the most about trusting people, for better or worse?", 3),
    ("a3", ["A3", "A6"], "Know me", ["acts"], 1,
     "What is the kindest thing I do that nobody asks me to?",
     "When did someone help you in a way you still think about?", 2),
    ("a4", ["A4"], "Deep", ["words"], 2,
     "When we disagree, what do I do first: argue, go quiet, or give in?",
     "What is one thing that would make our disagreements easier?", 3),
    ("a5", ["A5"], "Know me", ["words"], 1,
     "What am I secretly proud of that I rarely mention?",
     "What is something you are good at that you do not get credit for?", 2),
    ("c1", ["C1"], "Future", ["work", "planning"], 1,
     "What is the thing I am most confident I can handle?",
     "What is a problem you solved recently that you are quietly proud of?", 1),
    ("c2", ["C2"], "Everyday", ["home", "acts"], 1,
     "Which room would I tidy first if we had guests in an hour?",
     "What is one thing about how we keep the house that you would change?", 1),
    ("c3", ["C3", "A2"], "Everyday", ["money", "acts"], 1,
     "What rule do I follow that most people do not bother with?",
     "What promise are you proudest of having kept?", 2),
    ("c4", ["C4"], "Future", ["work", "planning"], 1,
     "What goal am I working toward right now, even if I have not said it out loud?",
     "What does doing enough look like to you, at work and at home?", 2),
    ("c5", ["C5"], "Everyday", ["home", "acts", "time"], 1,
     "What chore do I put off longer than any other?",
     "What gets you going on a day you would rather do nothing?", 1),
    ("c6", ["C6"], "Future", ["planning"], 1,
     "What is the most impulsive decision I have ever made?",
     "What is a snap decision you are glad you made?", 1),
]

STEMS = {
    "THIS_OR_THAT": ["Which is more me?", "Closer to the truth about me:", "Pick the one that sounds like me."],
    "SCALE": ["How much does this sound like me?", "How true is this of me?", "Would I say this about myself?"],
    "PICK_ONE": ["Which of these sounds most like me?", "Which one would I pick as most like me?"],
    "MULTI_SELECT": ["Pick the ones that sound like me.", "Which of these would I say describe me?"],
    "RANK": ["Order these from most like me to least.", "Rank these by how much they sound like me."],
    "DROPDOWN": ["Which one describes me best?", "If I had to pick one of these to describe me, which would it be?"],
}

SCALE_LABELS = [("Not me at all", "Completely me"), ("Not at all", "Very true"), ("Never", "Absolutely")]


def first_person(item):
    s = item.strip().rstrip(".")
    return "I " + s[0].lower() + s[1:]


# Statements per group, first person, exclusions applied.
POOL = {}
for key, ids, *_ in GROUPS:
    plus = [first_person(x) for f in ids for x in facets[f]["plus"] if x not in EXCLUDE]
    minus = [first_person(x) for f in ids for x in facets[f]["minus"] if x not in EXCLUDE]
    assert len(plus) >= 2 and len(minus) >= 2, (key, plus, minus)
    POOL[key] = (plus, minus)

by_domain = {}
for g in GROUPS:
    by_domain.setdefault(g[0][0], []).append(g[0])


def across(key, n, offset):
    """
    n statements, one per facet, starting from this group's own facet and
    walking the rest of the domain. Near-synonyms from one facet in the same
    list make "which is most me" a coin toss; a list of different traits is a
    real question. Positive-keyed first; a domain with fewer facets than n
    fills from the negative-keyed side of the next facets.
    """
    groups = by_domain[key[0]]
    k = groups.index(key)
    out = []
    for j in range(n):
        g = groups[(k + j) % len(groups)]
        plus, minus = POOL[g]
        side = plus if j < len(groups) else minus
        pick = side[(offset + j) % len(side)]
        if pick not in out:
            out.append(pick)
    assert len(out) == n, (key, out)
    return out


decks = {}
counter = Counter()
for n, (key, ids, category, tags, depth, guess, reflect, rdepth) in enumerate(GROUPS):
    plus, minus = POOL[key]
    names = " + ".join(facets[f]["name"] for f in ids)

    def q(suffix, qtype, text, d, **extra):
        counter[qtype] += 1
        return dict(id=f"ip{key}{suffix}", text=text, type=qtype, category=category, depth=d,
                    tags=tags, **extra)

    stem = lambda t: STEMS[t][n % len(STEMS[t])]
    low, high = SCALE_LABELS[n % len(SCALE_LABELS)]
    # Only the two prompts written for each trait. The statement lists read
    # like a personality test and were about one person, not two; the trait
    # survives as the idea behind a question asked in the app's own voice.
    questions = [
        q("g", "GUESS_ME", guess, depth),
        q("f", "REFLECT", reflect, rdepth),
    ]
    for x in questions:
        x["source"] = f"IPIP-NEO {names} ({', '.join(ids)})"
    decks.setdefault(key[0], []).extend(questions)

META = {
    "n": ("Steady and not", "DEEP"),
    "e": ("Out and about", "PLAYFUL"),
    "o": ("Curious", "KNOW_ME"),
    "a": ("Getting along", "KNOW_ME"),
    "c": ("Getting it done", "EVERYDAY"),
}
FULL = {"n": "neuroticism", "e": "extraversion", "o": "openness", "a": "agreeableness", "c": "conscientiousness"}

# ---- validation, mirroring QuizJson -------------------------------------
ids = set()
for domain, qs in decks.items():
    for x in qs:
        assert x["id"] not in ids, x["id"]; ids.add(x["id"])
        t, opts = x["type"], x.get("options", [])
        if t == "THIS_OR_THAT": assert len(opts) == 2
        elif t in ("REFLECT", "GUESS_ME", "SCALE"): assert not opts
        else: assert len(opts) >= 2
        assert len(set(o.lower() for o in opts)) == len(opts), x
        if "maxChoices" in x: assert 1 <= x["maxChoices"] <= len(opts)
        assert 1 <= x["depth"] <= 3
        for word in ("colour", "favourite", "organise", "realise", "behaviour", "centre"):
            assert word not in json.dumps(x).lower(), (x["id"], word)

for domain, qs in decks.items():
    title, theme = META[domain]
    deck = dict(
        id=f"daily-ipip-{FULL[domain]}", title=title, theme=theme,
        blurb="Daily only. Adapted from the International Personality Item Pool.",
        source="International Personality Item Pool (ipip.ori.org), public domain. "
               "Goldberg et al. (2006), Journal of Research in Personality, 40, 84-96.",
        questions=qs,
    )
    with io.open(OUT + deck["id"] + ".json", "w", encoding="utf-8", newline="\n") as f:
        json.dump(deck, f, indent=2, ensure_ascii=False)
        f.write("\n")

total = sum(len(v) for v in decks.values())
print("wrote", len(decks), "decks,", total, "questions")
print(dict(counter))
print("depth", Counter(x["depth"] for v in decks.values() for x in v))
print("category", Counter(x["category"] for v in decks.values() for x in v))
