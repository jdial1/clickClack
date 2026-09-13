"""
Questions for where you are and what the sky is doing.

    python tools/questions/local.py

Two folders:

- assets/weather/weather-{snow,rain,hot,cold}.json: asked on some days whose
  saved forecast is that kind of weather (see QuestionPool.daily).
- assets/regions/region-*.json: join the daily pool only for couples whose home
  ZIP is in that region. Local sports, food, weather and habits.

Original text. Teams are named because a question about "your team" in
Pittsburgh is a question about the Steelers, and pretending otherwise is
coyness, not neutrality. No question asks you to pick a side you might not
have: every sports question has a way out.
"""
import glob, io, json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "assets"))
TAGS = {"time", "words", "gifts", "acts", "touch", "planning", "money", "family",
        "friends", "home", "work", "past", "romance", "fun", "sports"}


def R(id, text, depth, tags, cat): return dict(id=id, text=text, type="REFLECT", depth=depth, tags=tags, category=cat)
def G(id, text, depth, tags, cat): return dict(id=id, text=text, type="GUESS_ME", depth=depth, tags=tags, category=cat)
def T(id, text, a, b, depth, tags, cat): return dict(id=id, text=text, type="THIS_OR_THAT", options=[a, b], depth=depth, tags=tags, category=cat)
def P(id, text, opts, depth, tags, cat): return dict(id=id, text=text, type="PICK_ONE", options=opts, depth=depth, tags=tags, category=cat)
def M(id, text, opts, depth, tags, cat, cap): return dict(id=id, text=text, type="MULTI_SELECT", options=opts, maxChoices=cap, depth=depth, tags=tags, category=cat)
def K(id, text, opts, depth, tags, cat): return dict(id=id, text=text, type="RANK", options=opts, depth=depth, tags=tags, category=cat)
def D(id, text, opts, depth, tags, cat): return dict(id=id, text=text, type="DROPDOWN", options=opts, depth=depth, tags=tags, category=cat)
def S(id, text, depth, tags, *rest):
    # (cat, low, high) or just (low, high), which defaults to Know me.
    cat, low, high = rest if len(rest) == 3 else ("Know me", *rest)
    return dict(id=id, text=text, type="SCALE", lowLabel=low, highLabel=high, depth=depth, tags=tags, category=cat)


WEATHER = {
    "snow": ("Snow day", "EVERYDAY", [
        T("wsn1", "Snow day:", "Out in it", "Under a blanket", 1, ["fun", "time"], "Everyday"),
        P("wsn2", "First thing I would do on a real snow day?", ["Build something", "Make hot chocolate", "Go back to bed", "Shovel before anyone else"], 1, ["home", "fun"], "Everyday"),
        G("wsn3", "What is my best snow memory from when I was a kid?", 1, ["past", "fun"], "Know me"),
        S("wsn4", "How much do I actually like snow, after the first hour?", 1, ["fun"], "Everyday", "Done with it", "Love it all day"),
        M("wsn5", "Pick what makes a snowed-in evening good.", ["A fire or a candle", "Soup", "A long movie", "A board game", "No phones"], 1, ["home", "time"], "Everyday", 3),
        R("wsn6", "If we were snowed in for three days, what would you want us to finally do?", 1, ["time", "home"], "Everyday"),
        D("wsn7", "Who would win a snowball fight between us?", ["Me, easily", "You, sadly", "Whoever throws first", "Nobody, we would call a truce", "The dog", "We would both get hit by a stranger"], 1, ["fun"], "Playful"),
        K("wsn8", "Put these winter chores in order of how much I dread them.", ["Shoveling", "Scraping the car", "Salting the steps", "Driving in it"], 1, ["home", "acts"], "Everyday"),
    ]),
    "rain": ("Rainy day", "EVERYDAY", [
        T("wrn1", "Rain on the window, I would rather:", "Nap", "Go for a walk in it", 1, ["time", "fun"], "Everyday"),
        P("wrn2", "The perfect rainy afternoon, if neither of us had anywhere to be?", ["A movie marathon", "Baking something", "Reading side by side", "Sleeping through it"], 1, ["time", "home"], "Everyday"),
        G("wrn3", "What song would I put on while it rains?", 1, ["fun"], "Know me"),
        S("wrn4", "How much does a gray day change my mood?", 1, ["words"], "Not at all", "Completely"),
        M("wrn5", "Pick the rainy-day comforts I would reach for.", ["Soup", "A hoodie", "A candle", "Tea or coffee", "A long shower"], 1, ["home"], "Everyday", 3),
        R("wrn6", "What is a rainy day of ours you remember, and why that one?", 2, ["past", "romance"], "Romantic"),
        D("wrn7", "Caught in a downpour with no umbrella, I would:", ["Run for it", "Walk slowly and accept it", "Dance in it", "Hide under the nearest awning", "Blame you", "Laugh the whole way"], 1, ["fun"], "Playful"),
        K("wrn8", "Put these in order of how I would spend a stormy night.", ["Watching the lightning", "Lights off, candles on", "Headphones and a show", "Checking the weather app"], 1, ["home", "time"], "Everyday"),
    ]),
    "hot": ("Hot day", "PLAYFUL", [
        T("wht1", "On a very hot day, I would rather:", "Find water", "Find air conditioning", 1, ["fun", "time"], "Playful"),
        P("wht2", "What is my go-to way to cool off?", ["Something frozen", "A cold shower", "Lying very still", "Complaining loudly"], 1, ["fun"], "Playful"),
        G("wht3", "What is the hottest place I have ever been, and what was I doing there?", 1, ["past", "fun"], "Travel"),
        S("wht4", "How grumpy do I get when it is this hot?", 1, ["fun"], "Not at all", "Very"),
        M("wht5", "Pick the summer things I would be happy to do tonight.", ["Eat outside", "Get ice cream", "Sit on the porch", "Swim", "Watch the sun go down"], 1, ["time", "fun"], "Playful", 3),
        R("wht6", "What is the best summer you have ever had, and what made it that?", 2, ["past"], "Know me"),
        D("wht7", "What would I pick for dinner on a night this hot?", ["Salad and nothing cooked", "Something grilled", "Cold noodles", "Takeout, no stove", "Watermelon counts", "Ice cream counts too"], 1, ["home", "fun"], "Food"),
        K("wht8", "Rank these by how good they sound right now.", ["A lake", "A pool", "An air-conditioned movie", "A shaded hammock"], 1, ["fun", "time"], "Playful"),
    ]),
    "cold": ("Cold day", "EVERYDAY", [
        T("wcd1", "When it is freezing, I would rather:", "Layer up and go out", "Stay in until spring", 1, ["time", "home"], "Everyday"),
        P("wcd2", "What warms me up fastest?", ["A hot drink", "A hot shower", "A blanket", "You"], 1, ["touch", "home"], "Everyday"),
        G("wcd3", "Which piece of winter clothing would I never give up?", 1, ["fun"], "Know me"),
        S("wcd4", "How cold do I run, compared to you?", 1, ["touch", "home"], "Always too warm", "Always freezing"),
        M("wcd5", "Pick what makes a cold night cozy for me.", ["Thick socks", "A heavy blanket", "Something baking", "Tea", "Sitting close"], 1, ["home", "touch"], "Everyday", 3),
        R("wcd6", "What is a cold-weather tradition you would like us to have?", 1, ["planning", "home"], "Everyday"),
        D("wcd7", "What would I cook on the coldest night of the year?", ["Chili", "Soup", "A roast", "Mac and cheese", "Something with a lot of garlic", "Nothing, we order in"], 1, ["home"], "Food"),
        K("wcd8", "Put these in order of how much I dislike them in the cold.", ["Getting out of bed", "A cold car", "Wet boots", "Short days"], 1, ["home"], "Everyday"),
    ]),
}

REGIONS = {
    "NEW_ENGLAND": ("New England", [
        T("rne1", "Game night in New England:", "Red Sox or Patriots on TV", "Anything but sports", 1, ["sports", "fun"], "Playful"),
        P("rne2", "Which New England sports memory would I bring up first?", ["A Red Sox World Series", "A Patriots Super Bowl", "The Celtics", "The Bruins", "I do not follow sports"], 1, ["sports", "past"], "Playful"),
        D("rne3", "Best New England food, according to me?", ["Lobster roll", "Clam chowder", "A real maple anything", "Fried clams", "Apple cider donuts", "Whoopie pie"], 1, ["fun"], "Food"),
        G("rne4", "Which New England town would I pick for a weekend away?", 1, ["planning"], "Travel"),
        S("rne5", "How much do I love leaf-peeping season?", 1, ["fun", "time"], "It is just leaves", "Best time of year"),
        R("rne6", "What is a New England tradition you want us to keep, or start?", 1, ["planning", "home"], "Everyday"),
        M("rne7", "Pick the New England outings I would say yes to this fall.", ["Apple picking", "A covered bridge drive", "A Fenway game", "The coast in the off-season", "A corn maze"], 1, ["fun", "time", "sports"], "Travel", 3),
        K("rne8", "Put these in order of how much I would miss them if we moved away.", ["The seasons", "The coast", "The food", "The accent"], 1, ["home"], "Know me"),
    ]),
    "MID_ATLANTIC": ("Mid-Atlantic", [
        T("rma1", "A big game day here, I would rather:", "Watch with a crowd", "Watch at home in peace", 1, ["sports", "friends"], "Playful"),
        P("rma2", "Which team would I defend in an argument?", ["A New York team", "A Philadelphia team", "A Pittsburgh team", "A Baltimore or Washington team", "I stay out of it"], 1, ["sports"], "Playful"),
        D("rma3", "Most important local food, according to me?", ["A proper slice of pizza", "A cheesesteak", "A bagel", "Crab cakes", "A pierogi", "Scrapple, honestly"], 1, ["fun"], "Food"),
        G("rma4", "Where would I take someone visiting here for the first time?", 1, ["friends", "planning"], "Travel"),
        S("rma5", "How much do I actually like the shore in summer?", 1, ["fun", "time"], "Too crowded", "Every weekend"),
        R("rma6", "What do you love most about living around here, and what would you change?", 1, ["home"], "Everyday"),
        M("rma7", "Pick the local outings I would say yes to.", ["A ballgame", "A day at the shore", "A museum", "A city food crawl", "A hike in the hills"], 1, ["fun", "sports", "time"], "Travel", 3),
        K("rma8", "Rank these by how much I would miss them if we moved.", ["The food", "The sports", "Being near a big city", "The four seasons"], 1, ["home", "sports"], "Know me"),
    ]),
    "SOUTH": ("The South", [
        T("rso1", "Saturday in the fall:", "College football, all day", "Anything else, please", 1, ["sports", "time"], "Playful"),
        P("rso2", "Which would I pick for a big Sunday?", ["NFL on TV", "A long church-and-lunch Sunday", "The lake", "Porch and a cold drink"], 1, ["sports", "time"], "Playful"),
        D("rso3", "Best Southern food, according to me?", ["Barbecue", "Biscuits and gravy", "Fried chicken", "Shrimp and grits", "Sweet tea counts", "Anything from a grandmother"], 1, ["fun", "family"], "Food"),
        G("rso4", "Which college team would I cheer for, even if I pretend not to care?", 1, ["sports"], "Playful"),
        S("rso5", "How well do I handle the humidity?", 1, ["fun"], "Barely", "Born for it"),
        R("rso6", "What Southern habit or saying do you want us to hold on to?", 1, ["words", "family"], "Everyday"),
        M("rso7", "Pick the outings I would say yes to this season.", ["A tailgate", "A crawfish boil or fish fry", "A day on the water", "A small-town festival", "A road trip to the coast"], 1, ["fun", "sports", "friends"], "Travel", 3),
        K("rso8", "Put these in order of what I would miss most if we moved north.", ["The weather", "The food", "The football", "The manners"], 1, ["home", "sports"], "Know me"),
    ]),
    "MIDWEST": ("The Midwest", [
        T("rmw1", "A Sunday in the fall:", "Football and a big pot of chili", "A drive to see the leaves", 1, ["sports", "time"], "Playful"),
        P("rmw2", "Which rivalry would I get loud about?", ["Bears and Packers", "Michigan and Ohio State", "Cubs and Cardinals", "A high school rivalry", "None, I am calm"], 1, ["sports"], "Playful"),
        D("rmw3", "Most Midwestern food, according to me?", ["A hotdish or casserole", "Deep dish pizza", "Cheese curds", "A pork tenderloin sandwich", "Chili over spaghetti", "Anything at a church supper"], 1, ["fun", "family"], "Food"),
        G("rmw4", "Which lake or river would I pick for a summer weekend?", 1, ["planning", "time"], "Travel"),
        S("rmw5", "How much do I enjoy a real Midwest winter?", 1, ["fun"], "Never again", "Bring it on"),
        R("rmw6", "What is a Midwest tradition you want us to keep, or start?", 1, ["planning", "family"], "Everyday"),
        M("rmw7", "Pick the outings I would say yes to.", ["A ballgame at an old stadium", "A county fair", "A lake cabin", "A small-town festival", "A drive through farm country"], 1, ["fun", "sports", "time"], "Travel", 3),
        K("rmw8", "Rank these by how much I would miss them if we moved away.", ["The people", "The seasons", "The sports", "The cost of living"], 1, ["home", "money", "sports"], "Know me"),
    ]),
    "MOUNTAIN": ("The Mountain West", [
        T("rmo1", "A free weekend out here:", "Up a mountain", "Down by the water", 1, ["fun", "time"], "Travel"),
        P("rmo2", "Which would I rather watch live?", ["A college football game", "An NBA or NHL game", "A rodeo", "A ski or snowboard event", "None, I would rather be out there"], 1, ["sports", "fun"], "Playful"),
        D("rmo3", "Best local food, according to me?", ["Green chile anything", "A breakfast burrito", "Fry sauce on everything", "Elk or bison burger", "Tacos from a truck", "Trail mix counts"], 1, ["fun"], "Food"),
        G("rmo4", "Which national park would I go back to first?", 1, ["planning", "past"], "Travel"),
        S("rmo5", "How much do I care about the altitude, honestly?", 1, ["fun"], "What altitude", "Every stair hurts"),
        R("rmo6", "What outdoor thing do you want us to finally try together out here?", 1, ["planning", "fun"], "Travel"),
        M("rmo7", "Pick the outings I would say yes to this season.", ["A hike to a view", "A hot spring", "Skiing or snowboarding", "A night under the stars", "A ballgame"], 1, ["fun", "sports", "time"], "Travel", 3),
        K("rmo8", "Put these in order of what I would miss most if we moved.", ["The mountains", "The sky", "The space", "The sunshine"], 1, ["home"], "Know me"),
    ]),
    "PACIFIC": ("The West Coast", [
        T("rpa1", "A perfect Saturday out here:", "The beach", "The mountains", 1, ["fun", "time"], "Travel"),
        P("rpa2", "Which would I rather go to?", ["A baseball game by the water", "An NBA game", "A soccer match with a loud crowd", "A surf competition", "None, a hike instead"], 1, ["sports", "fun"], "Playful"),
        D("rpa3", "Best West Coast food, according to me?", ["Fish tacos", "A burrito from the right place", "Fresh sushi", "Anything with avocado", "A food truck I cannot name", "Coffee counts as food"], 1, ["fun"], "Food"),
        G("rpa4", "Which stretch of the coast would I drive again tomorrow?", 1, ["planning", "past"], "Travel"),
        S("rpa5", "How much does the traffic ruin my mood?", 1, ["fun"], "Not at all", "Completely"),
        R("rpa6", "What do you want us to do out here before we ever move away?", 1, ["planning"], "Travel"),
        M("rpa7", "Pick the outings I would say yes to this month.", ["A sunset on the beach", "A wine or cider tasting", "A redwood walk", "A ballgame", "A farmers market"], 1, ["fun", "sports", "time"], "Travel", 3),
        K("rpa8", "Rank these by what I would miss most if we moved.", ["The ocean", "The weather", "The food", "The outdoors"], 1, ["home"], "Know me"),
    ]),
}

# Filled in with the nearest MLB and NFL teams to the home ZIP (see Teams.kt).
# Only drawn once a home ZIP is set, so the braces are never shown.
LOCAL_TEAMS = [
    T("rlt1", "Opening day for {mlb}:", "Worth a long lunch", "What is opening day", 1, ["sports", "fun"], "Playful"),
    S("rlt2", "How much do I care whether {nfl} win on Sunday?", 1, ["sports"], "Playful", "Not at all", "It ruins my week"),
    P("rlt3", "How would I most like to see {mlb} play?", ["In the stands with a hot dog", "On TV at home", "The radio in the car", "I would rather not"], 1, ["sports", "fun"], "Playful"),
    G("rlt4", "What is my honest opinion of {nfl}?", 1, ["sports", "words"], "Playful"),
    M("rlt5", "Pick what I would do for a {nfl} home game.", ["Tailgate", "Wear the jersey", "Watch at a bar", "Enjoy the empty stores", "Have people over"], 1, ["sports", "friends"], "Playful", 3),
    R("rlt6", "What is a game day you still think about, ours or from growing up?", 1, ["sports", "past"], "Playful"),
    K("rlt7", "Put these in order of what I enjoy most at a {mlb} game.", ["The food", "The game", "The people watching", "The seventh-inning stretch"], 1, ["sports", "fun"], "Playful"),
    D("rlt8", "If {nfl} made the Super Bowl, where would I watch it?", ["At home, just us", "A big party", "A sports bar", "At the game, somehow", "I would not watch", "Wherever the snacks are"], 1, ["sports", "friends"], "Playful"),
]

# More leagues. Each question is only asked while every league it names has a
# team within 300 miles and is in season (see League and Teams.activeLeagues).
LOCAL_TEAMS += [
    T("rlt9", "Playoff basketball with {nba}:", "Every game, loudly", "Just the big ones", 1, ["sports", "fun"], "Playful"),
    G("rlt10", "Which {nba} player would I most want to have dinner with?", 1, ["sports", "fun"], "Playful"),
    P("rlt11", "A night at a {nba} game, I would be most excited about:", ["Courtside seats", "The food", "The halftime show", "Getting on the big screen"], 1, ["sports", "fun"], "Playful"),
    T("rlt12", "A {nhl} game, I am there for:", "The hockey", "The fights and the organ music", 1, ["sports", "fun"], "Playful"),
    S("rlt13", "How much could I explain icing and offsides if you asked?", 1, ["sports", "words"], "Playful", "Not a word", "Diagrams included"),
    D("rlt14", "If {nhl} won the Cup, how would I celebrate?", ["Parade, obviously", "Quietly, with a beer", "Tell everyone at work", "Buy a jersey", "Call my family", "I would find out the next day"], 1, ["sports", "friends"], "Playful"),
    T("rlt15", "A {mls} match:", "In the supporters' section singing", "Somewhere with a seat and a view", 1, ["sports", "fun"], "Playful"),
    R("rlt16", "What sport would you want us to learn to love together?", 1, ["sports", "time"], "Playful"),
    P("rlt17", "On a {college} football Saturday, I am most likely:", ["At the game or the tailgate", "In front of the TV all day", "Checking the score now and then", "Enjoying the empty stores"], 1, ["sports", "time"], "Playful"),
    S("rlt18", "How much does a {college} loss ruin my weekend?", 1, ["sports"], "Playful", "Not at all", "Until Tuesday"),
    G("rlt19", "What is my best {college} game day memory?", 1, ["sports", "past"], "Playful"),
    M("rlt20", "Pick the local teams I would actually go see this year.", ["{mlb}", "{nfl}", "{nba}", "{nhl}", "{mls}", "{college} football"], 1, ["sports", "fun"], "Playful", 3),
]

# The light where you live, filled in from the home ZIP. Asked on every other
# day of that kind (by date), the days weather questions are not.
SUN = {
    "longest": ("Longest day", [
        T("sln1", "The longest day of the year, sunset at {sunset}. We should:", "Stay out until it is gone", "Go to bed like it is any other day", 1, ["time", "fun"], "Playful"),
        R("sln2", "What should we do with all this light tonight?", 1, ["time", "planning"], "Everyday"),
        G("sln3", "What is my favorite way to spend a long summer evening?", 1, ["time", "fun"], "Know me"),
        P("sln4", "The best place to watch the sun go down at {sunset}?", ["The porch", "A hill or a roof", "The water", "The car, windows down"], 1, ["time", "romance"], "Romantic"),
        S("sln5", "How much do I love these long light evenings?", 1, ["time"], "Everyday", "Too bright to sleep", "Best time of year"),
        R("sln6", "{daylight} of daylight today. What is one thing worth spending it on this summer?", 1, ["planning", "time"], "Everyday"),
    ]),
    "shortest": ("Shortest day", [
        T("ssh1", "The shortest day of the year. Tonight is for:", "Candles and staying in", "Going out anyway", 1, ["time", "home"], "Everyday"),
        R("ssh2", "Only {daylight} of daylight today. What helps you most in the dark part of the year?", 2, ["time", "words"], "Deep"),
        G("ssh3", "What do I miss most about summer right now?", 1, ["time"], "Know me"),
        P("ssh4", "The best part of a long winter night?", ["Sleeping in", "Soup and a movie", "Lights on the houses", "Nothing, it is too dark"], 1, ["home", "time"], "Everyday"),
        S("ssh5", "How much do the short days get to me?", 2, ["words"], "Deep", "Not at all", "More than I say"),
        R("ssh6", "From tomorrow the days get longer. What are you looking forward to as the light comes back?", 1, ["planning", "time"], "Everyday"),
    ]),
    "early_dark": ("Dark early", [
        T("sed1", "Dark by {sunset}. The evening is for:", "A walk before it goes", "Giving up and getting cozy", 1, ["time", "home"], "Everyday"),
        G("sed2", "What gets me through evenings that start this early?", 1, ["time", "words"], "Know me"),
        P("sed3", "The coziest way to spend a long dark evening?", ["A bath", "Cooking something slow", "A long game", "Bed by eight"], 1, ["home", "time"], "Everyday"),
        R("sed4", "What is one small thing we could do to make these dark evenings better?", 1, ["home", "planning"], "Everyday"),
        S("sed5", "How much do early sunsets change my mood?", 1, ["words"], "Everyday", "Not at all", "A lot"),
        M("sed6", "Pick what I would want on an evening dark by {sunset}.", ["A fire or candles", "Something warm to drink", "A show we both like", "Blankets", "You, nearby"], 1, ["home", "touch"], "Everyday", 3),
    ]),
    "late_light": ("Light late", [
        T("sll1", "Light until {sunset} tonight. After dinner:", "Back outside", "Couch, light or no light", 1, ["time", "fun"], "Everyday"),
        P("sll2", "What would I do with an extra hour of light tonight?", ["Walk somewhere", "Sit outside", "Work in the yard", "Nothing different, honestly"], 1, ["time", "home"], "Everyday"),
        G("sll3", "What is my favorite thing to do on a summer night after work?", 1, ["time", "work"], "Know me"),
        R("sll4", "Where would you want to watch the sun go down at {sunset}, if we went tonight?", 1, ["time", "romance"], "Romantic"),
        S("sll5", "How late do I like to stay out when it is still light?", 1, ["time", "fun"], "Home by dark", "As long as it lasts"),
        D("sll6", "The best food for an evening that stays light?", ["Something grilled", "Ice cream", "A picnic", "Takeout on the porch", "Whatever is cold", "Corn on the cob"], 1, ["fun", "home"], "Food"),
    ]),
}

existing_ids = set()
for f in glob.glob(os.path.join(ASSETS, "*", "*.json")):
    if os.path.basename(os.path.dirname(f)) in ("weather", "regions", "local", "sun"):
        continue
    for q in json.load(open(f, encoding="utf-8"))["questions"]:
        existing_ids.add(q["id"])


def check(qs):
    for q in qs:
        assert q["id"] not in existing_ids, q["id"]
        existing_ids.add(q["id"])
        assert "!" not in json.dumps(q), q["id"]
        t, opts = q["type"], q.get("options", [])
        if t == "THIS_OR_THAT": assert len(opts) == 2
        elif t in ("REFLECT", "GUESS_ME", "SCALE"): assert not opts
        else: assert len(opts) >= 2
        assert len({o.lower() for o in opts}) == len(opts), q
        if "maxChoices" in q: assert 1 <= q["maxChoices"] <= len(opts)
        assert set(q["tags"]) <= TAGS, q


def write(folder, deck_id, title, theme, qs, source):
    check(qs)
    os.makedirs(os.path.join(ASSETS, folder), exist_ok=True)
    deck = dict(id=deck_id, title=title, theme=theme, blurb="", source=source, questions=qs)
    with io.open(os.path.join(ASSETS, folder, deck_id + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(deck, f, indent=2, ensure_ascii=False)
        f.write("\n")


n = 0
for kind, (title, theme, qs) in WEATHER.items():
    write("weather", f"weather-{kind}", title, theme, qs, "Written for Click Clack")
    n += len(qs)
for region, (title, qs) in REGIONS.items():
    write("regions", f"region-{region.lower()}", title, "KNOW_ME", qs, "Written for Click Clack")
    n += len(qs)
write("local", "local-teams", "Local teams", "PLAYFUL", LOCAL_TEAMS, "Written for Click Clack")
n += len(LOCAL_TEAMS)
for kind, (title, qs) in SUN.items():
    write("sun", f"sun-{kind.replace('_', '-')}", title, "EVERYDAY", qs, "Written for Click Clack")
    n += len(qs)
print("wrote", n, "questions")
