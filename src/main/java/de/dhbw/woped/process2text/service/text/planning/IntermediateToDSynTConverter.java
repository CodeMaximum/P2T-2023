package de.dhbw.woped.process2text.service.text.planning;

import de.dhbw.woped.process2text.model.Pair;
import de.dhbw.woped.process2text.model.dsynt.DSynTMainSentence;
import de.dhbw.woped.process2text.model.intermediate.AbstractFragment;
import de.dhbw.woped.process2text.model.intermediate.ConditionFragment;
import de.dhbw.woped.process2text.model.intermediate.ExecutableFragment;
import de.dhbw.woped.process2text.service.text.planning.record.ModifierRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IntermediateToDSynTConverter {
  public static final int VERB_TYPE_MAIN = 0;
  public static final int VERB_TYPE_CONDITION = 1;
  public static final int VERB_TYPE_SUBCONDITION = 2;

  /** Create DSynT representation for verb. Returns the according DSynT element. */
  public static Element createVerb(Document doc, AbstractFragment frag, int verbType) {
    Element verb = doc.createElement("dsyntnode");
    verb.setAttribute("class", "verb");

    String action = frag.getAction();
    String[] splitAction;
    if (action.contains(" ")) {
      splitAction = action.split(" ");
      verb.setAttribute("lexeme", splitAction[0]);
      Element verbAdd = doc.createElement("dsyntnode");
      verbAdd.setAttribute("class", "adverb");
      verbAdd.setAttribute("lexeme", splitAction[1]);
      verbAdd.setAttribute("position", "post-verbal");
      verbAdd.setAttribute("rel", "ATTR");
      verb.appendChild(verbAdd);
    } else {
      verb.setAttribute("lexeme", action);
    }

    // Check attributes
    if (frag.verbIsPassive) {
      verb.setAttribute("voice", "pass");
    }
    if (frag.verbIsParticiple) {
      verb.setAttribute("mood", "pres-part");
    }
    if (frag.verbIsPast) {
      verb.setAttribute("tense", "past");
    }
    if (frag.verbIsNegated) {
      verb.setAttribute("polarity", "neg");
    }
    if (frag.verbIsImperative) {
      verb.setAttribute("mood", "imp");
    }

    switch (verbType) {
      case VERB_TYPE_MAIN:
        verb.setAttribute("rel", "nil");
        break;
      case VERB_TYPE_CONDITION:
        if (((ConditionFragment) frag).senHeadPosition) {
          verb.setAttribute("starting_point", "+");
          verb.setAttribute("rel", "ATTR");
        } else {
          verb.setAttribute("rel", "ATTR");
        }
        break;
      case VERB_TYPE_SUBCONDITION:
        verb.setAttribute("rel", "II");
        break;
    }
    return verb;
  }

  /** Create DSynT representation for business object. Returns the according DSynt element. */
  public static Element createBO(Document doc, AbstractFragment frag) {
    Element object = doc.createElement("dsyntnode");
    if (!frag.getBo().equals("") && !frag.boReplaceWithPronoun) {
      object.setAttribute("class", "common_noun");
      if (frag.boIsSubject) {
        object.setAttribute("rel", "I");
      } else {
        object.setAttribute("rel", "II");
      }
      if (frag.boIsPlural) {
        object.setAttribute("number", "pl");
      }
      if (frag.boHasArticle) {
        object.setAttribute("article", "def");
        if (frag.boHasIndefArticle) {
          object.setAttribute("article", "indef");
        }
      } else {
        object.setAttribute("article", "no");
      }
      object.setAttribute("lexeme", frag.getBo());
    }
    if (frag.boReplaceWithPronoun) {
      object.setAttribute("class", "common_noun");
      object.setAttribute("rel", "I");
      object.setAttribute("article", "def");
      object.setAttribute("lexeme", "dummy");
      object.setAttribute("pro", "pro");
      object.setAttribute("gender", "neut");
    }
    return object;
  }

  /**
   * Iterates over the list of modification records of the fragment and implements them in the DSynT
   * representation.
   *
   * @param verbRoot: affected verb
   * @param objectRoot: affected object
   */
  public static void appendMods(
      Document doc, AbstractFragment frag, Element verbRoot, Element objectRoot) {
    Element lastVerbMod = null;
    for (String modS : frag.getAllMods()) {
      Element mod = doc.createElement("dsyntnode");
      switch (frag.getModType(modS)) {
        case ModifierRecord.TYPE_ADV:
          mod.setAttribute("class", "adverb");
          break;
        case ModifierRecord.TYPE_ADJ:
          mod.setAttribute("class", "adjective");
          break;
        case ModifierRecord.TYPE_QUANT:
          mod.setAttribute("class", "quantifier");
          objectRoot.setAttribute("article", "no");
          break;
        case ModifierRecord.TYPE_PREP:
          mod.setAttribute("punct", "comma");
          break;
      }
      mod.setAttribute("rel", "ATTR");
      mod.setAttribute("lexeme", modS);
      for (Pair<String, String> att : frag.getModOptions(modS)) {
        mod.setAttribute(att.fst, att.snd);
      }
      switch (frag.getModTarget(modS)) {
        case ModifierRecord.TARGET_VERB:
          if (lastVerbMod == null) {
            verbRoot.appendChild(mod);
            lastVerbMod = mod;
          } else {
            lastVerbMod.appendChild(mod);
            lastVerbMod = mod;
          }
          break;
        case ModifierRecord.TARGET_ROLE:
          objectRoot.appendChild(mod);
          break;
        case ModifierRecord.TARGET_BO:
          objectRoot.appendChild(mod);
          break;
      }
      // FINISH: implement for other elements
    }
  }

  /** Create DSynT representation for role. Returns the according DSynt element. */
  public static Element createRole(Document doc, AbstractFragment frag) {
    Element role = doc.createElement("dsyntnode");

    if (AbstractFragment.DEM_PRONOUNS.contains(frag.getRole())) {
      role.setAttribute("class", "demonstrative_pronoun");
    } else if (AbstractFragment.PRONOUNS.contains(frag.getRole())) {
      role.setAttribute("class", "proper_noun");
    } else {
      role.setAttribute("class", "proper_noun");
      role.setAttribute("article", "def");
    }

    if (frag.roleIsImperative) {
      role.setAttribute("pro", "pro");
      role.setAttribute("person", "2nd");
    }

    role.setAttribute("rel", "I");
    role.setAttribute("lexeme", frag.getRole());

    if (frag.verbIsPassive) {
      Element by = doc.createElement("dsyntnode");
      by.setAttribute("rel", "ATTR");
      by.setAttribute("lexeme", "BY");
      by.appendChild(role);
      return by;
    }
    return role;
  }

  /** Adds addition to given DSynT representation */
  public static void createAddition(Document doc, Element root, AbstractFragment frag) {
    Element prep = doc.createElement("dsyntnode");
    Element add = doc.createElement("dsyntnode");

    String prepLex = getPrepFromAddition(frag.getAddition());
    String addLex = getAdditionOnly(frag.getAddition());

    prep.setAttribute("rel", "ATTR");
    prep.setAttribute("lexeme", prepLex.toLowerCase());
    prep.setAttribute("class", "subordinating_conj");
    root.appendChild(prep);

    add.setAttribute("class", "common_noun");
    add.setAttribute("rel", "II");
    add.setAttribute("lexeme", addLex);
    if (addLex.endsWith("ing") || !frag.addHasArticle) {
      add.setAttribute("article", "no");
    } else {
      add.setAttribute("article", "def");
    }
    prep.appendChild(add);
  }

  public static void createAddSentences(Document doc, Element root, ExecutableFragment eFrag) {
    for (ExecutableFragment frag : eFrag.getSentencList()) {
      DSynTMainSentence s = new DSynTMainSentence(frag);
      s.createDSynTRepresentation();

      if (frag.senIsCoord) {
        Element add = doc.createElement("dsyntnode");
        add.setAttribute("rel", "COORD");
        add.setAttribute("lexeme", "AND");
        root.appendChild(add);
        Element v = (Element) doc.importNode(s.getVerb(), true);
        v.setAttribute("rel", "II");
        add.appendChild(v);
      } else {
        Element v = (Element) doc.importNode(s.getVerb(), true);
        v.setAttribute("rel", "ATTR");
        root.appendChild(v);
      }
    }
  }

  private static String getPrepFromAddition(String addition) {
    return addition.split(" ")[0].toUpperCase();
  }

  private static String getAdditionOnly(String addition) {
    String[] addSplit = addition.split(" ");
    StringBuilder s = new StringBuilder();
    for (int i = 1; i < addSplit.length; i++) {
      s.append(" ").append(addSplit[i]);
    }
    s = new StringBuilder(s.toString().trim());
    return s.toString().toLowerCase();
  }

  public static Element createConditionNode(Document doc, ConditionFragment cFrag) {
    if (cFrag.getType() != ConditionFragment.TYPE_NONE) {
      Element ifNode = doc.createElement("dsyntnode");
      ifNode.setAttribute("rel", "COORD");
      switch (cFrag.getType()) {
        case ConditionFragment.TYPE_IF:
          ifNode.setAttribute("lexeme", "IF");
          break;
        case ConditionFragment.TYPE_AS_LONG_AS:
          ifNode.setAttribute("lexeme", "as long as");
          break;
        case ConditionFragment.TYPE_ONCE:
          ifNode.setAttribute("lexeme", "once");
          break;
        case ConditionFragment.TYPE_WHETHER:
          ifNode.setAttribute("lexeme", "whether");
          break;
        case ConditionFragment.TYPE_WHEN:
          ifNode.setAttribute("lexeme", "when");
          break;
        case ConditionFragment.TYPE_IN_CASE:
          ifNode.setAttribute("lexeme", "in case");
          break;
      }
      return ifNode;
    } else {
      return null;
    }
  }

  public static void insertConnective(Document doc, Element verb, String lemma) {
    Element mod = doc.createElement("dsyntnode");
    mod.setAttribute("adv-type", "sentential");
    mod.setAttribute("rel", "ATTR");
    mod.setAttribute("lexeme", lemma);
    mod.setAttribute("punct", "comma");
    verb.appendChild(mod);
  }
}
