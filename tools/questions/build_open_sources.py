"""
Daily questions from openly licensed research sources.

    python tools/questions/build_open_sources.py

Sources, and why each one may be used:

- IPIP-VIA character strengths, Barchard's emotional-intelligence components,
  IPIP-HEXACO, ORVIS and ORAIS: International Personality Item Pool, public
  domain "for any purpose, commercial or non-commercial" (ipip.ori.org).
- American Time Use Survey activity lexicon: US Bureau of Labor Statistics,
  public domain. Activity names are reworded into plain language.
- Relational Behavior Interactions scale: Harel & Koslowsky (2024), Scientific
  Reports 14, doi:10.1038/s41598-024-58901-2, CC BY 4.0. Adapted; credit required.
- CFPB Financial Well-Being Scale: Consumer Financial Protection Bureau, public
  domain. Reworded gently for a couple.

Every group of statements yields one question of each of the eight types, then
further rounds of the six list-and-scale types until each usable statement has
led a question. List options come from different groups, so a list holds
different traits or activities rather than near-synonyms. Items that read as
accusations, clinical items, politics and religion, and dated technology are
left out.
"""
import glob, io, json, os, re
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
OUT = os.path.join(ASSETS, "daily")
DATA = json.load(open(os.path.join(HERE, "ipip_more.json"), encoding="utf-8"))
NEO = json.load(open(os.path.join(ROOT, "tools", "ipip", "ipip_neo300.json"), encoding="utf-8"))

TAGS = {"time", "words", "gifts", "acts", "touch", "planning", "money", "family",
        "friends", "home", "work", "past", "romance", "fun"}
MAX_LIST_OPTION = 64  # longer statements still lead a scale, but crowd a list


def norm(s):
    s = s.lower().strip().rstrip(".")
    return re.sub(r"^i ", "", s)


# ---- statements ----------------------------------------------------------

FIXES = {
    "Don't act is if I'm a special person": "Don't act as if I'm a special person",
    "Weigh the pro's and the con's": "Weigh the pros and cons",
    "When interacting with a group of people, am often bothered by at least one of them":
        "When I am with a group of people, I am often bothered by at least one of them",
    "Enjoy being thought of as a normal \"mainstream\" person": "Enjoy being thought of as a mainstream person",
    "Enjoy feeling \"close to the earth\"": "Enjoy feeling close to the earth",
    "Want everything to be \"just right\"": "Want everything to be just right",
    "Can always say \"enough is enough\"": "Can always say enough is enough",
}


def first_person(item):
    item = FIXES.get(item, item).strip().rstrip(".")
    if item.startswith("When I am"):
        return item
    return "I " + item[0].lower() + item[1:]


TRAIT_DROP = {
    # VIA
    "Take advantage of others", "Feel isolated from other people", "Have difficulty accepting love from anyone",
    "Am not fun to be with", "Lie to get myself out of trouble", "Feel like an imposter",
    "Find few things in my life to be grateful for", "Do not see the need to acknowledge others who are good to me",
    "Try not to do favors for others", "Am only kind to others if they have been kind to me",
    "Behave in unusual and strange ways", "Dread getting up in the morning", "Like to exaggerate my troubles",
    "Am committed to principles of justice and equality",
    # EI
    "Shout or scream when I'm angry", "Sympathize with the homeless",
    "Believe that criminals should receive help rather than punishment", "Have no sympathy for criminals",
    "Look down on any weakness", "Have little sympathy for the unemployed", "Believe that the poor deserve our sympathy",
    "Am unaffected by the suffering of others", "Feel little concern for others",
    # HEXACO
    "Am mainly interested in money", "Am out for my own personal gain", "Have a strong need for power",
    "Would like to have more power than other people", "Believe that I am better than others", "Boast about my virtues",
    "Get back at people who insult me", "Get even with others", "Speak ill of others", "Find fault with everything",
    "Rarely feel depressed", "Often feel blue", "Make a fool of myself", "Find political discussions interesting",
    "Don't bother worrying about political and social problems", "Would fear walking in a high-crime part of a city",
    "Am likely to show off if I get the chance", "Distrust people", "Feel that most people can't be trusted",
}
DROP_GROUPS = {"Spirituality/Religiousness", "Sincerity", "Fairness"}

# key, category, tags, list depth, guess-me, reflect, reflect depth
VIA = {
    "Appreciation of Beauty": ("beauty", "Know me", ["fun", "time"], 1,
        "What is the most beautiful place I have ever been?",
        "What is something ordinary that you find beautiful?", 1),
    "Capacity for Love": ("love", "Deep", ["words", "romance", "touch"], 2,
        "Who, besides you, makes me feel most loved?",
        "When did you first feel sure that I loved you?", 3),
    "Citizenship/Teamwork": ("team", "Know me", ["friends", "work"], 1,
        "What team, club or group have I loved being part of?",
        "When do the two of us work best as a team?", 2),
    "Curiosity": ("curious", "Know me", ["fun"], 1,
        "What have I been curious about lately?",
        "What is a question you have wanted to ask me for a while?", 3),
    "Equity/Fairness": ("fair", "Deep", ["words"], 2,
        "What do I think is the fairest way to settle a disagreement?",
        "When have you felt treated unfairly, and how did you handle it?", 2),
    "Forgiveness/Mercy": ("forgive", "Deep", ["words", "past"], 2,
        "What is something I forgave that I never thought I would?",
        "What makes an apology feel real to you?", 3),
    "Gratitude": ("thanks", "Deep", ["words", "acts"], 1,
        "What am I most grateful for this week?",
        "What is something small I did lately that you were thankful for?", 2),
    "Hope/Optimism": ("hope", "Future", ["planning"], 1,
        "What am I most hopeful about for next year?",
        "What keeps you going when things look bleak?", 3),
    "Humor/Playfulness": ("humor", "Playful", ["fun"], 1,
        "What is the funniest thing that happened to me this year?",
        "What joke would only the two of us get?", 1),
    "Industry/Perseverance/Persistence": ("grit", "Future", ["work", "planning"], 1,
        "What is a project I refused to give up on?",
        "What is something you want to finish this year?", 2),
    "Integrity/Honesty/Authenticity": ("honest", "Deep", ["words"], 2,
        "What value would I never compromise on?",
        "When is honesty hardest for you?", 3),
    "Judgment/Open-mindedness": ("judge", "Know me", ["planning", "words"], 1,
        "What big decision did I agonize over the longest?",
        "What did you change your mind about after hearing the other side?", 2),
    "Kindness/Generosity": ("kind", "Know me", ["acts", "friends"], 1,
        "Who did I last go out of my way to help?",
        "What is the kindest thing a stranger has done for you?", 1),
    "Leadership": ("lead", "Know me", ["work", "friends"], 1,
        "When have I ended up in charge without meaning to?",
        "Who is the best leader you have known, and what made them good?", 2),
    "Love of Learning": ("learn", "Know me", ["fun"], 1,
        "What would I study if I could go back to school for free?",
        "What did you learn recently that you cannot stop thinking about?", 1),
    "Modesty/Humility": ("humble", "Know me", ["words"], 1,
        "What accomplishment of mine do I downplay the most?",
        "When is it hard for you to accept a compliment?", 2),
    "Originality/Creativity": ("original", "Playful", ["fun"], 1,
        "What is the most creative thing I have ever made?",
        "What would you make with a free month and no budget?", 1),
    "Perspective/Wisdom": ("wise", "Deep", ["words"], 2,
        "What is the best advice I have ever been given?",
        "What do you know now that you wish you had known ten years ago?", 3),
    "Prudence": ("careful", "Future", ["planning"], 1,
        "What risk would I never take, no matter the payoff?",
        "When has being careful paid off for you?", 1),
    "Self-regulation/Self-control": ("habits", "Everyday", ["home"], 1,
        "What habit do I keep trying to break?",
        "What habit would you like us to build together?", 2),
    "Social/Personal/Emotional Intelligence": ("social", "Know me", ["friends"], 1,
        "Who do I find easiest to talk to, besides you?",
        "How can you tell when I need something I have not asked for?", 2),
    "Valor/Bravery/Courage": ("brave", "Deep", ["words"], 2,
        "What is the bravest thing I have ever done?",
        "What do you want to be braver about?", 3),
    "Zest/Enthusiasm/Vitality": ("zest", "Playful", ["fun", "time"], 1,
        "What gets me out of bed excited?",
        "What are you looking forward to right now?", 1),
}

EI = {
    "POSITIVE EXPRESSIVITY": ("glad", "Know me", ["touch", "words"], 1,
        "How do I show I am happy without saying it?",
        "How do you most like to be shown affection when we are out?", 2),
    "NEGATIVE EXPRESSIVITY": ("upset", "Deep", ["words"], 2,
        "How can you tell I am upset before I say anything?",
        "Which is easier for you to show: sadness or anger?", 3),
    "ATTENDING TO EMOTIONS": ("notice", "Deep", ["words"], 2,
        "What feeling do I have the most trouble putting into words?",
        "How are you really feeling today, in one honest sentence?", 2),
    "EMOTION-BASED DECISION-MAKING": ("heart", "Future", ["planning"], 1,
        "What big decision did I make with my heart instead of my head?",
        "When should we trust our gut over the numbers?", 2),
    "RESPONSIVE JOY": ("joy", "Playful", ["fun", "friends"], 1,
        "Whose good news makes me happiest?",
        "What good news from this week have you not told me yet?", 1),
    "RESPONSIVE DISTRESS": ("moved", "Deep", ["words"], 2,
        "What kind of movie scene always gets to me?",
        "What is the best way to comfort you when you are upset?", 2),
    "EMPATHIC CONCERN": ("care", "Know me", ["acts"], 1,
        "What cause would I give my time to if I had more of it?",
        "Who in our lives could use some extra care right now?", 2),
}

HEXACO = {
    "Greed Avoidance": ("greed", "Future", ["money"], 1,
        "What would I buy first with a surprise ten thousand dollars?",
        "What does having enough money mean to you?", 2),
    "Modesty": ("modest", "Know me", ["words"], 1,
        "What do I think I am better at than most people?",
        "What compliment from me would mean the most to you?", 2),
    "Fearfulness": ("fear", "Playful", ["fun"], 1,
        "What scary thing would I never try, even on a dare?",
        "What fear have you gotten over as an adult?", 2),
    "Anxiety": ("worry", "Deep", ["words"], 2,
        "What do I do to calm down when I am on edge?",
        "What are you worrying about that I could help with?", 2),
    "Dependence": ("lean", "Deep", ["words", "touch"], 2,
        "When do I most need reassurance from you?",
        "How do you like to be supported when things are hard?", 3),
    "Sentimentality": ("sentiment", "Deep", ["past", "words"], 1,
        "What keepsake would I grab first in a fire?",
        "What movie, song or book always makes you emotional?", 1),
    "Expressiveness": ("talk", "Playful", ["words", "fun"], 1,
        "What could I talk about for hours at a party?",
        "What conversation of ours do you remember best?", 2),
    "Social Boldness": ("bold", "Know me", ["friends"], 1,
        "Would I rather give the toast or hide in the kitchen?",
        "When have you surprised yourself by speaking up?", 2),
    "Sociability": ("sociable", "Playful", ["friends"], 1,
        "Who would I invite to a dinner party for six?",
        "How much time with friends feels right for us in a month?", 2),
    "Liveliness": ("lively", "Everyday", ["time"], 1,
        "What time of day do I have the most energy?",
        "What gives you energy when you are running low?", 1),
    "Forgiveness": ("letgo", "Deep", ["words", "past"], 2,
        "How long does it take me to get over a disagreement?",
        "Is there anything between us you would like to let go of?", 3),
    "Gentleness": ("gentle", "Know me", ["words"], 1,
        "What pet peeve do I let slide the most?",
        "What is something about me you have learned to accept, and even like?", 2),
    "Flexibility": ("flex", "Everyday", ["home"], 1,
        "What household arrangement would I defend to the end?",
        "Where are you willing to compromise more than I realize?", 2),
    "Patience": ("patience", "Everyday", ["time"], 1,
        "What makes me lose patience fastest?",
        "When have you been more patient with me than I noticed?", 2),
    "Organization": ("tidy", "Everyday", ["home"], 1,
        "Which drawer or closet of mine is secretly the messiest?",
        "What would make our home feel more organized without more work?", 1),
    "Diligence": ("diligent", "Future", ["work"], 1,
        "What task do I throw myself into the hardest?",
        "How do you know when you have worked enough for one day?", 2),
    "Perfectionism": ("detail", "Everyday", ["home", "work"], 1,
        "What detail do I notice that nobody else does?",
        "Where is good enough good enough for you?", 2),
    "Prudence": ("plans", "Future", ["planning", "money"], 1,
        "What plan of mine went exactly the way I wanted?",
        "What is a plan we should make before the year is out?", 1),
    "Aesthetic Appreciation": ("aesthetic", "Know me", ["fun"], 1,
        "What piece of music can I get completely lost in?",
        "What place looks beautiful to you in every season?", 1),
    "Inquisitiveness": ("explore", "Travel", ["fun", "planning"], 1,
        "What strange place would I love to explore?",
        "What topic would you like us to learn about together?", 1),
    "Creativity": ("ideas", "Playful", ["fun"], 1,
        "What is my best idea that I never followed through on?",
        "What could we make together, just for fun?", 1),
    "Unconventionality": ("quirky", "Playful", ["fun"], 1,
        "What is the quirkiest thing about me?",
        "What unusual tradition would you like us to start?", 1),
}

# ---- activities ----------------------------------------------------------

PAST_TO_BASE = {
    "ate": "eat", "went": "go", "chose": "choose", "spent": "spend", "worked": "work", "bought": "buy",
    "traded": "trade", "tried": "try", "produced": "produce", "wrote": "write", "acted": "act",
    "painted": "paint", "played": "play", "sang": "sing", "attended": "attend", "visited": "visit",
    "lifted": "lift", "used": "use", "exercised": "exercise", "did": "do", "participated": "participate",
    "sold": "sell", "purchased": "purchase", "chewed": "chew", "ordered": "order", "cared": "care",
    "gardened": "garden", "planted": "plant", "transplanted": "transplant", "picked": "pick",
    "composted": "compost", "walked": "walk", "rode": "ride", "changed": "change", "recycled": "recycle",
    "washed": "wash", "made": "make", "cleaned": "clean", "ironed": "iron", "cooked": "cook", "baked": "bake",
    "knitted": "knit", "quilted": "quilt", "sewed": "sew", "crocheted": "crochet", "listened": "listen",
    "had": "have", "planned": "plan", "entertained": "entertain", "fed": "feed", "bathed": "bathe",
    "groomed": "groom", "donated": "donate", "volunteered": "volunteer", "studied": "study",
    "learned": "learn", "enrolled": "enroll", "shopped": "shop", "discussed": "discuss", "watched": "watch",
    "took": "take", "stayed": "stay", "traveled": "travel", "looked": "look", "raced": "race",
    "dined": "dine", "wore": "wear", "put": "put", "drank": "drink", "read": "read",
}
DOUBLE = {"plan", "shop", "put", "run", "swim", "get", "sit", "set", "chat", "knit", "quit"}


def gerund(verb):
    if verb in ("be", "see"): return verb + "ing"
    if verb.endswith("ie"): return verb[:-2] + "ying"
    if verb in DOUBLE: return verb + verb[-1] + "ing"
    if verb.endswith("e") and not verb.endswith("ee"): return verb[:-1] + "ing"
    return verb + "ing"


def reform(phrase, fn):
    """Apply fn to each verb that starts the phrase or follows ', ' / ' or '."""
    words = phrase.split(" ")
    out = []
    for i, w in enumerate(words):
        bare = w.rstrip(",").lower()
        lead = i == 0 or words[i - 1] == "or" or words[i - 1].endswith(",")
        if lead and bare in PAST_TO_BASE:
            new = fn(PAST_TO_BASE[bare])
            out.append(new + ("," if w.endswith(",") else ""))
        else:
            out.append(w)
    s = " ".join(out)
    return s[0].upper() + s[1:]


class Act:
    """One activity in the three forms a question needs."""
    def __init__(self, past=None, base=None, ing=None):
        self.past = past
        self.base = base
        self.ing = ing

    @property
    def key(self):
        return (self.ing or self.base or self.past).lower()


def orais_act(item):
    return Act(past=item, base=reform(item, lambda v: v), ing=reform(item, gerund))


def orvis_act(item):
    item = item.replace("one's house", "my house").replace("data base", "database")
    words = item.split(" ")
    ing = " ".join([gerund(words[0].lower()).capitalize()] + words[1:])
    ing = ing.replace(" or draw", " or drawing")
    return Act(base=item, ing=ing)


def plain(ing):
    return Act(ing=ing)


ORAIS_DROP_GROUPS = {"Child-Related", "Computing", "Drinking", "Gambling", "Political/Organizational",
                     "Religious/Spiritual Practices", "Social-Networking"}
ORAIS_DROP = {
    "Downloaded music from the Internet", "Traded music with a friend", "Shopped in a music store",
    "Used an mp3 player or Ipod", "Checked the sales ads in a newspaper", "Read newspaper ads for non-grocery items",
    "Used eBay to buy or sell something", "Looked something up in an encyclopedia",
    "Read the editorial page of a newspaper", "Read a news magazine", "Recorded a television program",
    "Obtained stock market prices", "Purchased a commodity as an investment", "Spent 10 minutes or more in a non-grocery store",
    "Used both sides of a piece of paper before discarding it", "Watched too much television", "Ate too much",
    "Went fishing or hunting", "Wrote poetry",  # poetry sits in Creativity already
    # Real activities, but nobody ranks them as a favorite or picks one for a Saturday.
    "Spent more than 10 minutes thinking about what to wear", "Spent more than an hour thinking about what to wear",
    "Ate food while walking or working", "Ate or drank while driving", "Picked up litter", "Bought or sold real estate",
    "Read a fashion-related book", "Bought a self-help book", "Bought a book about the things that I collect",
    "Chewed gum", "Bought something other than groceries", "Spent an hour or more in a non-grocery store",
    "Purchased a musical album", "Recycled one or more items", "Watched television news",
    "Listened to music on the radio", "Read music-related news", "Used a computer for social networking",
}

ORAIS = {
    "Being Alone": ("alone", "Know me", ["time"], 1,
        "What do I like to do when I have the house to myself?",
        "How much time alone do you need in a week to feel like yourself?", 2),
    "Collecting": ("collect", "Playful", ["fun"], 1,
        "What would I collect if space and money were no object?",
        "What have you kept from childhood, and why?", 1),
    "Creativity": ("create", "Playful", ["fun"], 1,
        "What is the last completely new thing I tried?",
        "What new thing should we try this month?", 1),
    "Culture": ("culture", "Travel", ["fun", "time"], 1,
        "What museum, show or performance would I travel to see?",
        "What is the best live performance you have ever seen?", 1),
    "Exercise": ("exercise", "Everyday", ["time"], 1,
        "What workout would I pick if I had to do one forever?",
        "What would help us stay active together?", 1),
    "Fashion": ("fashion", "Know me", ["fun"], 1,
        "What piece of clothing would I never throw away?",
        "Which outfit of mine is your favorite?", 1),
    "Financial": ("invest", "Future", ["money", "planning"], 1,
        "What is the smartest money move I have made?",
        "What money goal do you want us to work on together?", 2),
    "Food-Related": ("snack", "Food", ["fun"], 1,
        "What is my go-to delivery order?",
        "What restaurant should we go back to soon?", 1),
    "Game-Playing": ("games", "Playful", ["fun"], 1,
        "Which game am I most competitive about?",
        "What game should we play together more often?", 1),
    "Gardening": ("garden", "Everyday", ["home"], 1,
        "What would I plant if we had a big yard?",
        "What would make our space feel more alive?", 1),
    "Green Activities": ("green", "Everyday", ["home", "planning"], 1,
        "What eco-friendly habit do I stick to?",
        "What is one habit we could change to waste less?", 1),
    "Housekeeping": ("house", "Everyday", ["home", "acts"], 1,
        "What household job am I secretly good at?",
        "What chore do you wish we did together instead of apart?", 1),
    "Music": ("music", "Playful", ["fun"], 1,
        "What song would I pick for a road trip sing-along?",
        "What song reminds you of us?", 1),
    "Partying": ("party", "Playful", ["friends", "fun"], 1,
        "Who would I invite first to a party at our place?",
        "What would our perfect night hosting friends look like?", 1),
    "Pets": ("pets", "Everyday", ["home", "fun"], 1,
        "What pet would I get if there were no rules?",
        "What animal would best fit our household, and why?", 1),
    "Reading": ("read", "Know me", ["fun"], 1,
        "What book have I recommended the most?",
        "What should we read together next?", 1),
    "Romance": ("romance", "Spicy", ["romance"], 1,
        "What was my favorite date we have ever been on?",
        "What date would you like us to plan next?", 1),
    "Self-Improvement": ("improve", "Future", ["planning", "work"], 1,
        "What skill am I trying to learn right now?",
        "What would you like to get better at this year?", 1),
    "Shopping": ("shop", "Everyday", ["money", "gifts"], 1,
        "What store could I spend an hour in without noticing?",
        "What purchase turned out to be worth every penny?", 1),
    "Sports": ("sports", "Playful", ["fun", "friends"], 1,
        "What team or sport would I watch no matter what?",
        "What sport would you like us to try together?", 1),
    "Summer Activities": ("summer", "Travel", ["time", "fun"], 1,
        "What is my idea of a perfect summer day?",
        "What should be on our list for next summer?", 1),
    "Travel": ("trips", "Travel", ["planning", "fun"], 1,
        "What trip would I take again tomorrow?",
        "What trip should we start saving for?", 1),
    "TV": ("tv", "Everyday", ["time", "home"], 1,
        "What show would I rewatch from the beginning?",
        "What should we watch together next?", 1),
    "Understanding": ("lookup", "Know me", ["words"], 1,
        "What do I look up most often?",
        "What did you read or watch lately that made you think?", 1),
    "Vehicles": ("ride", "Playful", ["fun"], 1,
        "What is my dream car?",
        "What road trip would you take in any vehicle you wanted?", 1),
    "Writing/Remembering": ("remember", "Deep", ["past", "words"], 1,
        "What memory of ours would I put on the first page of a scrapbook?",
        "What moment from this year do you want to remember?", 2),
}

ORVIS_DROP = {"Organize a political campaign", "Be a state governor or senator", "Run for political office",
              "Be a minister, priest, rabbi or other", "Instruct parents on child care", "Be a bounty hunter",
              "Manage a computer data base"}
ORVIS = {
    "Leadership": ("lead", "Future", ["work"], 1,
        "What would I change first if I ran my workplace?",
        "Where in our life together would you like to take the lead more?", 2),
    "Organization": ("organize", "Future", ["work", "money", "planning"], 1,
        "What system of mine would I recommend to everyone?",
        "How should we split planning our money and our calendar?", 2),
    "Altruism": ("help", "Future", ["work", "acts"], 1,
        "Who would I most like to help, if I could help anyone?",
        "What kind of help feels most meaningful for you to give?", 2),
    "Creativity": ("make", "Playful", ["fun", "work"], 1,
        "If I could pick a talent for free, what would it be?",
        "What creative thing would you like to do more of?", 1),
    "Analysis": ("solve", "Know me", ["work"], 1,
        "What puzzle or problem could I work on for hours?",
        "What about how the world works still amazes you?", 1),
    "Production": ("build", "Everyday", ["home", "work"], 1,
        "What would I build or grow if we had more space?",
        "What would you like to make with your hands?", 1),
    "Adventure": ("dare", "Travel", ["fun"], 1,
        "What adventure is at the top of my list?",
        "What is the most physically challenging thing you want to try?", 1),
    "Erudition": ("words", "Know me", ["words"], 1,
        "What language would I learn overnight if I could?",
        "What book would you like me to read, and why?", 1),
}

# American Time Use Survey lexicon, in plain words. Groups follow its tiers.
ATUS = [
    ("inside", "Everyday", ["home", "acts"], 1, "chores",
     ["Cleaning the house", "Doing laundry", "Doing the dishes", "Putting away groceries", "Mending clothes",
      "Rearranging furniture", "Fixing things around the house", "Changing the sheets"],
     "Which chore do I think I do more of than you realize?", "Which chore should we swap for a month?", 1),
    ("kitchen", "Food", ["home", "acts"], 1, "chores",
     ["Cooking dinner", "Setting the table", "Cleaning up the kitchen", "Grocery shopping", "Packing lunches",
      "Planning meals for the week", "Making breakfast"],
     "What is my signature dish when it is my turn to cook?", "How should we split cooking and cleaning up?", 1),
    ("outside", "Everyday", ["home", "acts"], 1, "chores",
     ["Mowing the lawn", "Weeding the garden", "Washing the car", "Taking the car in for service", "Running errands",
      "Taking out the trash", "Shoveling snow", "Watering the plants"],
     "Which errand do I actually enjoy running?", "What errand could we turn into a date?", 1),
    ("running", "Everyday", ["planning", "money", "home"], 1, "chores",
     ["Paying the bills", "Planning the week", "Sorting the mail", "Answering messages", "Keeping the calendar",
      "Locking up at night", "Feeding the pets", "Walking the dog"],
     "What household task do I keep track of that you might not notice?",
     "What part of running the house feels invisible to you?", 2),
    ("downtime", "Everyday", ["time", "fun"], 1, "fun",
     ["Watching TV", "Listening to music", "Playing games", "Reading for fun", "Arts and crafts", "Working on a hobby",
      "Just sitting and thinking", "Writing for fun"],
     "What is my favorite way to spend a free evening at home?", "What does real rest look like for you?", 1),
    ("goingout", "Playful", ["fun", "friends"], 1, "fun",
     ["Going to a concert", "Seeing a play", "Visiting a museum", "Going to the movies", "Going to a party",
      "Catching up with friends", "Hosting people at our place", "Going to a class or talk"],
     "What night out would I pick for my birthday?", "What is happening around town that we should go to?", 1),
    ("playing", "Playful", ["fun", "time"], 1, "fun",
     ["Walking", "Running", "Biking", "Hiking", "Doing yoga", "Lifting weights", "Dancing", "Swimming", "Golfing",
      "Bowling", "Fishing", "Skiing or snowboarding", "Playing tennis", "Playing basketball"],
     "What sport was I best at growing up?", "What activity could we get better at together?", 1),
    ("watching", "Playful", ["fun", "friends"], 1, "fun",
     ["Watching football", "Watching basketball", "Watching baseball", "Watching soccer", "Watching hockey",
      "Watching golf", "Watching tennis", "Watching gymnastics", "Watching figure skating", "Watching auto racing"],
     "What game or event would I pay the most to see live?", "What sports tradition do you want us to have?", 1),
]

# Harel & Koslowsky (2024), Table 1, reworded to talk to your partner. CC BY 4.0.
HAREL = [
    ("close", "Know me", ["romance", "touch", "words"], 1, [
        ("Hug you", "Hugged you"), ("Praise you for something you did", "Praised you for something you did"),
        ("Tell you I love you", "Told you I love you"), ("Show you how much I love you", "Showed you how much I love you"),
        ("Spend time with you", "Spent time with you"), ("Make you feel good", "Made you feel good"),
        ("Compliment you", "Complimented you")],
     "What did I do for you this week that you might not have noticed?",
     "What did I do this week that made your day better?", 1),
    ("showup", "Everyday", ["acts", "home"], 1, [
        ("Go along with something you needed", "Went along with something you needed"),
        ("Give you space for what matters to you", "Gave you space for what matters to you"),
        ("Stick to something we agreed on", "Stuck to something we agreed on"),
        ("Do my share around the house", "Did my share around the house"),
        ("Keep us close after a disagreement", "Kept us close after a disagreement"),
        ("Think of you in my everyday choices", "Thought of you in my everyday choices"),
        ("Help when you asked", "Helped when you asked")],
     "Which of our agreements do I take most seriously?",
     "What small thing could I do more of next week?", 1),
    ("open", "Deep", ["words", "planning"], 2, [
        ("Talk about our plans and goals", "Talked about our plans and goals"),
        ("Tell you how special we are", "Told you how special we are"),
        ("Ask you for help", "Asked you for help"),
        ("Tell you what I need", "Told you what I need"),
        ("Share my feelings with you", "Shared my feelings with you"),
        ("Talk with you about all kinds of things", "Talked with you about all kinds of things")],
     "What have I been wanting to talk to you about?",
     "What would you like us to talk about more openly?", 2),
]

# ---- stems -----------------------------------------------------------------

STEMS = {
    "trait": {
        "THIS_OR_THAT": ["Which is more me?", "Closer to the truth about me:", "Pick the one that sounds like me.",
                         "Which would I say fits me better?"],
        "SCALE": ["How much does this sound like me?", "How true is this of me?", "Would I say this about myself?",
                  "How well does this describe me?"],
        "PICK_ONE": ["Which of these sounds most like me?", "Which one would I pick as most like me?",
                     "Which of these would my friends say fits me best?"],
        "MULTI_SELECT": ["Pick the ones that sound like me.", "Which of these would I say describe me?",
                         "Which of these are true of me?"],
        "RANK": ["Order these from most like me to least.", "Rank these by how much they sound like me.",
                 "Put these in order, most me first."],
        "DROPDOWN": ["Which one describes me best?", "If I had to pick one of these to describe me, which would it be?",
                     "Which of these is the most me?"],
    },
    "jobs": {
        "THIS_OR_THAT": ["Which would I rather do all day?", "Which would I pick for a career?"],
        "SCALE": ["How much would I enjoy {x}?", "How good would I be at {x}?"],
        "PICK_ONE": ["Which of these would I be best at?", "Which of these would I try first?"],
        "MULTI_SELECT": ["Pick the ones I would enjoy doing for a living.", "Which of these would I be good at?"],
        "RANK": ["Rank these from dream job to no thanks.", "Order these by how much I would enjoy them."],
        "DROPDOWN": ["If money did not matter, which would I spend my days doing?",
                     "Which of these would I secretly love to try?"],
    },
    "activities": {
        "THIS_OR_THAT": ["Which would I rather do this weekend?", "Which have I done more often this year?"],
        "SCALE": ["How much do I enjoy {x}?", "How often do I find time for {x}?"],
        "PICK_ONE": ["Which of these did I do most recently?", "Which of these have I done most often?"],
        "MULTI_SELECT": ["Which of these have I done in the past year?", "Which of these have I done more than once?"],
        "RANK": ["Rank these by how much I enjoy them.", "Order these from favorite to least favorite for me."],
        "DROPDOWN": ["Which of these would I pick for a free Saturday?", "Which of these do I do most often?"],
        "GUESS_ME": ["When did I last {x}?", "Tell me about the last time I got to {x}."],
    },
    "chores": {
        "THIS_OR_THAT": ["Which would I rather do?", "Given the choice, I would take:"],
        "SCALE": ["How much do I mind {x}?", "How much do I actually enjoy {x}?"],
        "PICK_ONE": ["Which of these would I hand off first?", "Which of these do I secretly not mind?"],
        "MULTI_SELECT": ["Which of these do I usually take care of?", "Which of these did I do this week?"],
        "RANK": ["Rank these from least to most dreaded, for me.", "Order these by how much I would rather skip them."],
        "DROPDOWN": ["Which of these would I do happily every day?", "Which of these do I think I am best at?"],
        "GUESS_ME": ["How do I feel about {x}, honestly?"],
    },
    "fun": {
        "THIS_OR_THAT": ["Which would I rather do tonight?", "Pick my Saturday:"],
        "SCALE": ["How much do I enjoy {x}?", "How much would I like more time for {x}?"],
        "PICK_ONE": ["Which of these would I pick first?", "Which of these would I most like to do with you?"],
        "MULTI_SELECT": ["Which of these do I enjoy?", "Which of these would I say yes to this month?"],
        "RANK": ["Rank these by how much I enjoy them.", "Order these from favorite to least favorite for me."],
        "DROPDOWN": ["Which of these would I pick on a free afternoon?", "Which of these would I never get tired of?"],
        "GUESS_ME": ["What do I love about {x}?"],
    },
    "harel": {
        "THIS_OR_THAT": ["Which did I do more this week?"],
        "SCALE": ["How much did I {x} this week?"],
        "PICK_ONE": ["Which of these did I do most this week?"],
        "MULTI_SELECT": ["Which of these did I do this week?"],
        "RANK": ["Rank these by how much they matter to me."],
        "DROPDOWN": ["Which of these comes easiest to me?"],
    },
}

SCALE_LABELS = {
    "trait": [("Not me at all", "Completely me"), ("Not at all", "Very true"), ("Never", "Absolutely")],
    "jobs": [("Not at all", "Loads"), ("Terrible", "Great")],
    "activities": [("Not my thing", "Love it"), ("Never", "All the time")],
    "chores": [("Don't mind it", "Dread it"), ("Not at all", "Really do")],
    "fun": [("Not my thing", "Love it"), ("Not really", "Much more")],
    "harel": [("Not at all", "Very much")],
}

# ---- engine ----------------------------------------------------------------

counter = Counter()
LETTER = {"THIS_OR_THAT": "t", "SCALE": "s", "PICK_ONE": "p", "MULTI_SELECT": "m",
          "RANK": "k", "DROPDOWN": "d", "GUESS_ME": "g", "REFLECT": "f"}


class Group:
    def __init__(self, key, category, tags, depth, guess, reflect, rdepth, items, minus=None):
        assert set(tags) <= TAGS, (key, tags)
        self.key, self.category, self.tags, self.depth = key, category, tags, depth
        self.guess, self.reflect, self.rdepth = guess, reflect, rdepth
        self.items = items            # leading side (trait: + keyed; activities: all)
        self.minus = minus or []      # other side, traits only


def build(prefix, family, groups, deck_id, title, theme, source):
    stems = STEMS[family]
    labels = SCALE_LABELS[family]
    questions = []
    asked = set()

    def text_of(item, form):
        if family == "trait":
            return item
        return getattr(item, form) or item.ing or item.base

    def fits_list(item, form):
        return len(text_of(item, form)) <= MAX_LIST_OPTION

    def across(gi, lead, n, offset, form):
        """lead first, then one from each other group, cycling; unique, list-sized."""
        if not fits_list(lead, form):
            own = [x for x in (groups[gi].items + groups[gi].minus) if fits_list(x, form)]
            lead = own[offset % len(own)] if own else lead
        out = [lead]
        seen = {text_of(lead, form).lower()}
        step = 0
        while len(out) < n and step < 200:
            g = groups[(gi + 1 + step) % len(groups)]
            pool = [x for x in (g.items + g.minus) if fits_list(x, form)] or (g.items + g.minus)
            cand = pool[(offset + step) % len(pool)]
            if text_of(cand, form).lower() not in seen:
                out.append(cand); seen.add(text_of(cand, form).lower())
            step += 1
        assert len(out) == n, (prefix, groups[gi].key, n)
        return [text_of(x, form) for x in out]

    for gi, g in enumerate(groups):
        pairs = [(g.items[i], g.minus[i]) for i in range(min(len(g.items), len(g.minus)))]
        queue = g.items + g.minus
        r = 0
        while True:
            n = gi + r
            stem = lambda t: stems[t][n % len(stems[t])]

            def q(t, text, d, **extra):
                # The same stem over the same options is the same question twice.
                sig = (t, text.lower(), tuple(sorted(o.lower() for o in extra.get("options", []))))
                if sig in asked:
                    return
                asked.add(sig)
                counter[t] += 1
                questions.append(dict(id=f"{prefix}{g.key}{LETTER[t]}{r}",
                                      text=text, type=t, category=g.category, depth=d, tags=g.tags, **extra))

            lead = queue[r % len(queue)]
            # this or that: a + keyed and a - keyed statement, or two groups' activities
            if family == "trait" and r < len(pairs):
                a, b = pairs[r]
            else:
                a = g.items[r % len(g.items)]
                other = groups[(gi + 1 + r) % len(groups)]
                b = (other.items + other.minus)[r % len(other.items + other.minus)]
            form_tot = {"activities": "base", "harel": "past"}.get(family, "ing")
            if text_of(a, form_tot).lower() != text_of(b, form_tot).lower():
                q("THIS_OR_THAT", stem("THIS_OR_THAT"), g.depth, options=[text_of(a, form_tot), text_of(b, form_tot)])

            low, high = labels[n % len(labels)]
            sitem = queue[(r * 7 + 1) % len(queue)] if r else (g.items[1] if len(g.items) > 1 else g.items[0])
            if family == "trait":
                q("SCALE", f"{stem('SCALE')} “{sitem}.”", g.depth, lowLabel=low, highLabel=high)
            else:
                form = "base" if family == "harel" else "ing"
                x = text_of(sitem, form)
                x = x if family == "harel" else x[0].lower() + x[1:]
                q("SCALE", stem("SCALE").format(x=x.lower() if family == "harel" else x), g.depth,
                  lowLabel=low, highLabel=high)

            pick_form = {"activities": "past", "harel": "past"}.get(family, "ing")
            q("PICK_ONE", stem("PICK_ONE"), g.depth, options=across(gi, lead, 4, 2 + r, pick_form))
            multi_form = {"activities": "past", "harel": "past"}.get(family, "ing")
            q("MULTI_SELECT", stem("MULTI_SELECT"), g.depth, options=across(gi, queue[(r + 1) % len(queue)], 5, 3 + r, multi_form),
              maxChoices=3)
            q("RANK", stem("RANK"), g.depth, options=across(gi, queue[(r + 2) % len(queue)], 4, 4 + r, "ing"))
            drop_form = "ing"
            q("DROPDOWN", stem("DROPDOWN"), g.depth, options=across(gi, queue[(r + 3) % len(queue)], 6, 1 + r, drop_form))

            if r == 0:
                q("GUESS_ME", g.guess, g.depth)
                q("REFLECT", g.reflect, g.rdepth)
            elif "GUESS_ME" in stems:
                item = queue[r % len(queue)]
                x = text_of(item, "base" if family in ("activities", "jobs") else "ing")
                q("GUESS_ME", stem("GUESS_ME").format(x=x[0].lower() + x[1:]), g.depth)

            r += 1
            if r * 4 >= len(queue):  # each round leads with four statements
                break

    for x in questions:
        x["source"] = source
    deck = dict(id=deck_id, title=title, theme=theme,
                blurb="Daily only.", source=source, questions=questions)
    return deck


def trait_groups(scales, table, taken):
    groups = []
    for s in scales:
        if s["name"] in DROP_GROUPS or s["name"] not in table:
            continue
        def keep(items):
            out = []
            for i in items:
                if i in TRAIT_DROP or i.startswith("Note.") or re.search(r"\(\d{4}\)", i):
                    continue
                k = norm(first_person(i))
                if k in taken:
                    continue
                taken.add(k); out.append(first_person(i))
            return out
        plus, minus = keep(s["plus"]), keep(s["minus"])
        if len(plus) + len(minus) < 3:
            print("  skipped (too few left):", s["name"]); continue
        if not plus:
            plus, minus = minus, []
        key, cat, tags, depth, guess, reflect, rdepth = table[s["name"]]
        groups.append(Group(key, cat, tags, depth, guess, reflect, rdepth, plus, minus))
    return groups


# ---- run -------------------------------------------------------------------

taken = {norm("I " + x[0].lower() + x[1:]) for f in NEO for x in f["plus"] + f["minus"]}
decks = []

decks.append(build("iv", "trait", trait_groups(DATA["via"], VIA, taken), "daily-ipip-strengths",
                   "Strengths", "DEEP", "IPIP-VIA character strengths (ipip.ori.org), public domain"))
decks.append(build("ie", "trait", trait_groups(DATA["ei"], EI, taken), "daily-ipip-feelings",
                   "Feelings", "DEEP", "IPIP emotional intelligence components, Barchard (2001), public domain"))
decks.append(build("ih", "trait", trait_groups(DATA["hexaco"], HEXACO, taken), "daily-ipip-hexaco",
                   "Six sides", "KNOW_ME", "IPIP-HEXACO, Ashton, Lee & Goldberg (2007), public domain"))

orvis = []
for s in DATA["orvis"]:
    key, cat, tags, depth, guess, reflect, rdepth = ORVIS[s["name"]]
    acts = [orvis_act(i) for i in s["plus"] if i not in ORVIS_DROP]
    orvis.append(Group(key, cat, tags, depth, guess, reflect, rdepth, acts))
decks.append(build("io", "jobs", orvis, "daily-ipip-work", "Work you would love", "FUTURE",
                   "Oregon Vocational Interest Scales, Pozzebon et al. (2010), public domain"))

orais = []
for s in DATA["orais"]:
    if s["name"] in ORAIS_DROP_GROUPS:
        continue
    key, cat, tags, depth, guess, reflect, rdepth = ORAIS[s["name"]]
    acts = [orais_act(i) for i in s["plus"] if i not in ORAIS_DROP]
    orais.append(Group(key, cat, tags, depth, guess, reflect, rdepth, acts))
decks.append(build("ia", "activities", orais, "daily-ipip-pastimes", "Pastimes", "PLAYFUL",
                   "Oregon Avocational Interest Scales, Goldberg (2010), public domain"))

for family, deck_id, title, theme in (("chores", "daily-atus-chores", "Who does what", "EVERYDAY"),
                                      ("fun", "daily-atus-free-time", "Free time", "PLAYFUL")):
    groups = [Group(k, c, t, d, g, rf, rd, [plain(x) for x in items])
              for k, c, t, d, fam, items, g, rf, rd in ATUS if fam == family]
    decks.append(build("bt", family, groups, deck_id, title, theme,
                       "American Time Use Survey activity lexicon, US Bureau of Labor Statistics, public domain"))

harel = [Group(k, c, t, d, g, rf, rd, [Act(base=b, past=p, ing=b) for b, p in items])
         for k, c, t, d, items, g, rf, rd in HAREL]
# RANK and DROPDOWN read better as "Hugging you"; build the -ing form from the base verb.
for g in harel:
    for a in g.items:
        w = a.base.split(" ")
        a.ing = " ".join([gerund(w[0].lower()).capitalize()] + w[1:])
decks.append(build("hk", "harel", harel, "daily-couple-week", "This week", "EVERYDAY",
                   "Relational Behavior Interactions scale, Harel & Koslowsky (2024), Scientific Reports, "
                   "doi:10.1038/s41598-024-58901-2, CC BY 4.0, adapted"))

CFPB_SOURCE = "CFPB Financial Well-Being Scale, Consumer Financial Protection Bureau, public domain, reworded"
F = dict(category="Future", tags=["money", "planning"], source=CFPB_SOURCE)
cfpb = [
    dict(id="cf1", type="SCALE", text="How ready would I feel for a big unexpected expense?", depth=2,
         lowLabel="Not ready", highLabel="Completely ready", **F),
    dict(id="cf2", type="SCALE", text="How much do I feel we are building a secure future?", depth=2,
         lowLabel="Not yet", highLabel="Very much", **F),
    dict(id="cf3", type="SCALE", text="How often does money keep me from something I want?", depth=2,
         lowLabel="Never", highLabel="All the time", **F),
    dict(id="cf4", type="SCALE", text="How much does the way we handle money let me enjoy life?", depth=2,
         lowLabel="Not much", highLabel="A lot", **F),
    dict(id="cf5", type="THIS_OR_THAT", text="Right now, I feel like we are:", depth=2,
         options=["Getting ahead", "Just getting by"], **F),
    dict(id="cf6", type="SCALE", text="How much do I worry that our savings will not last?", depth=2,
         lowLabel="Not at all", highLabel="A lot", **F),
    dict(id="cf7", type="THIS_OR_THAT", text="A gift for a friend's wedding is:", depth=1,
         options=["Easy to fit in", "Something to plan for"], **F),
    dict(id="cf8", type="PICK_ONE", text="At the end of a typical month, I think we have:", depth=2,
         options=["Plenty left over", "A little left over", "About nothing left", "Less than we need"], **F),
    dict(id="cf9", type="SCALE", text="How on top of our finances do I feel?", depth=2,
         lowLabel="Behind", highLabel="On top of it", **F),
    dict(id="cf10", type="SCALE", text="How much does money set the pace of my life?", depth=2,
         lowLabel="Not at all", highLabel="Completely", **F),
    dict(id="cf11", type="RANK", text="Rank these money goals by how much they matter to me.", depth=2,
         options=["Handling a surprise expense", "Saving for the future", "Enjoying life now",
                  "Having money left each month"], **F),
    dict(id="cf12", type="MULTI_SELECT", text="Which of these would make me feel better about money?", depth=2,
         options=["A bigger emergency fund", "A budget we both use", "Less debt", "A plan for retirement",
                  "A regular money check-in together"], maxChoices=2, **F),
    dict(id="cf13", type="GUESS_ME", text="What money worry do I carry that I do not talk about much?", depth=3, **F),
    dict(id="cf14", type="REFLECT", text="What would make talking about money easier for us?", depth=2, **F),
    dict(id="cf15", type="DROPDOWN", text="What would I do first with an extra $500 a month?", depth=1,
         options=["Save it", "Pay down debt", "Put it toward a trip", "Spend it on the house", "Invest it",
                  "Treat us both"], **F),
]
for x in cfpb:
    counter[x["type"]] += 1
decks.append(dict(id="daily-cfpb-money", title="Money, gently", theme="FUTURE", blurb="Daily only.",
                  source=CFPB_SOURCE, questions=cfpb))

# ---- validation, mirroring QuizJson -------------------------------------------

existing_ids, existing_text = set(), set()
ours = {d["id"] for d in decks}
for f in glob.glob(os.path.join(ASSETS, "quizzes", "*.json")) + glob.glob(os.path.join(OUT, "*.json")):
    d = json.load(open(f, encoding="utf-8"))
    if d["id"] in ours:
        continue
    for q in d["questions"]:
        existing_ids.add(q["id"]); existing_text.add(q["text"].lower())

ids = set()
for d in decks:
    for x in d["questions"]:
        assert x["id"] not in ids and x["id"] not in existing_ids, x["id"]
        ids.add(x["id"])
        t, opts = x["type"], x.get("options", [])
        if t == "THIS_OR_THAT": assert len(opts) == 2, x
        elif t in ("REFLECT", "GUESS_ME", "SCALE"): assert not opts, x
        else: assert len(opts) >= 2, x
        assert len(set(o.lower() for o in opts)) == len(opts), x
        if "maxChoices" in x: assert 1 <= x["maxChoices"] <= len(opts)
        assert 1 <= x["depth"] <= 3
        assert set(x["tags"]) <= TAGS
        if t in ("REFLECT", "GUESS_ME"):
            assert x["text"].lower() not in existing_text, ("repeat prompt", x["text"])
            existing_text.add(x["text"].lower())
        blob = json.dumps(x).lower()
        for word in ("colour", "favourite", "organise", "realise", "behaviour", "centre", "travelling"):
            assert word not in blob, (x["id"], word)

for d in decks:
    with io.open(os.path.join(OUT, d["id"] + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write("\n")

print("wrote", len(decks), "decks,", sum(len(d["questions"]) for d in decks), "questions")
for d in decks:
    print(" ", d["id"], len(d["questions"]))
print(dict(counter))
