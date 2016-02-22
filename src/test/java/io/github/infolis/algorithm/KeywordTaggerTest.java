package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.Keyword;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class KeywordTaggerTest extends InfolisBaseTest {

    @Test
    public void evaluateSearchResultsFromOneSource() throws IOException {
        
        Entity p = new Entity();
        p.setIdentifier("xyz");
        p.setName("abc");
        p.setAbstractText("Actual living conditions and subjectively perceived quality of life ofthe population. Cumulation of the welfare surveys 1978 to 1993.Topics: 1. Assessment of future prospects: satisfaction with standardof living; pessimistic or optimistic expectations regarding developmentof selected areas of life; optimistic future expectation.2. Political attitudes and political participation: personal politicalinfluence; interest in politics; satisfaction with possibilities ofpolitical activity; conservative or progressive attitude to life;postmaterialism; party preference.3. Attitudes to social inequality and the welfare state: perceivedlines of conflict between social groups; satisfaction with democracy,public safety, environmental protection and the social net; generalfeeling of happiness; contentment with life in comparison over timewith past and future; frequency of personal unemployment and judgementon financial security in case of unemployment.4. Family and education: understanding with partner and satisfactionwith partnership (interviewer rating: presence of partner whileanswering this question); satisfaction with education; desire for othereducation; satisfaction with status as housewife and reasons for workas housewife; judgement on the chances of new contacts; disturbance ofwell-being from differences of opinion in one´s family or quarrel withneighbors, friends or colleagues; importance of selected areas of lifefor well-being and general contentment with life; importance ofoccupation, leisure time and family; work asking too much.5. Attitude to abortion, the child benefit, aid to those wanting todie and health in general: attitude to the child benefit and the legalregulation of abortion; attitude to aid to those wanting to die; healthsatisfaction (scale); visits to the doctor in recent time; life changesand occupation changes due to illness or disability; use of medication;characterization of psychological and physical situation (scale).6. Importance classification of areas of life and occupationalcharacteristics: satisfaction with housing (scale); judgement onresidence quality and satisfaction with residential area (scale);contentment with life (scale); satisfaction with housekeeping;satisfaction with leisure time; extent of leisure time; satisfactionwith personal career; satisfaction with income in comparison over timeand comparison with friends as well as average income; assessment ofpersonal job market chances; most important demands of an ideal job andcomparison of these criteria with current situation at work;satisfaction with division of work in household; percent division ofwork in household among respondent, spouse, children and furthermembers of household; perceived burden of respondent from occupationand household as well as assumed burden of spouse; further paid sidejobs or further primary occupation; unpaid primary occupation andunpaid side jobs as well as paid casual work and help with neighbors,friends and relatives; paid agricultural activity and honoraryactivities; experiencing a particularly happy or unfortunate event inlife; division and carrying out activities in household as well asrepair jobs, renovation work, improvement work by members of household,relatives, neighbors or businesses; assistance from relatives, friendsand neighbors.7. Religiousness and devotion: frequency of church attendance andsatisfaction with church.8. Perceived environmental pollution: perceived environmentalpollution from noise, air pollution, shortage of green areas anddestruction of countryside; concern about water pollution, harm to theocean, air pollution, improper elimination of chemical industry wastesand the elimination of radioactive waste; judgement on the purity oftap water.$9. Anomia.10. Demographic information on person of respondent: questions on thearea nationality, length of stay, residence, place of residence,further information on composition of household and children ofrespondent, self-assessment of social class, short time work, furthereducation, reasons for not being employed, expected improvement inincome situation; perceived possibilities to find work; desire foremployment; duration of entire employment and after discontinuation ofemployment; fear of unemployment and change of position; occupationalprestige scale; social origins; number of rooms; toilet and bath withinresidence; central heating; residential status; amount of rent; type ofbuilding; living in high-rise; type of city; size of municipality;satisfaction with babies in household; someone in need of nursing carein household; children in home or care facility; other relatives inhome or care facility; friends or friendships; frequency of meetingwith friends.$11. Data on friends and acquaintances.12. Memberships in organizations and clubs: union membership; partymembership; membership in citizen initiatives, church clubs, sportclubs as well as choral societies; involvement in the club throughrequest to speak.13. Information on course of interview: place of interview; presenceof other persons during interview and their degree of relationship torespondent; intervention of others in interview; interruption ofinterview; willingness to cooperate and credibility of respondent;start of interview; end of interview.");
        dataStoreClient.post(Entity.class, p);
        
        Entity p2 = new Entity();
        p2.setIdentifier("xyz2");
        p2.setName("abc2");
        p2.setAbstractText("This round of Eurobarometer surveys sought public opinion on issues oftime usage and product safety instructions.Topics: Respondents were asked about their current employment statusand occupation, matters pertaining to work arrangements and leaveoptions such as teleworking, work schedule flexibility, and takingsabbaticals, as well as activities that have an impact on their freetime. Respondents were asked about their satisfaction with respect tovarious aspects of life including their job, health, and financialsituation. Respondents were queried on the number of hours they workedper week, whether or not they intended to reduce their working hoursand for how long, and what they would do with their extra free time.Respondents were also asked for their opinions about stress levels atwork, compensation, working conditions, and job security. They werealso asked at what age they would like to retire and at what age theyexpected to retire, whether they would consider postponing retirementfor any reason, and what they would do with their extra time uponretiring. Other questions were asked about professional trainingcompleted by the respondents in the previous 12 months, whether theytook time off of work to complete the training, who should beresponsible for paying for such training and about their attitudestowards lifelong learning. Respondents were asked how many childrenunder the age of 14 were living in their household and if thechildren´s grandparents ever looked after them and with what frequency.In addition, respondents with grandchildren were asked if they everlooked after their grandchildren and whether or not they did so on aregular basis. Respondents also were asked a series of questionsregarding product safety information with respect to ´Do-it-yourself´(DIY) products. Respondents were shown different logos that hadappeared on DIY products and were asked if they were familiar with thevarious logos and if they knew what the logos said about the product.Respondents were asked if they took safety logos or other safetyinformation into account when purchasing DIY products, whether or notthey read instructions accompanying DIY products, whether they keptinstructions for future use, where they thought the best location forinstructions would be, whether they preferred safety instructions to beconveyed by logos or text, and whether or not safety information forDIY products was generally useful. Similarly, they were asked aboutproduct safety information regarding toys and other products forchildren. Respondents were also asked whether they took safetyinformation into account when buying toys or children´s products,whether they read safety instructions, and whether they kept safetyinstructions for future reference. Further, they were asked if they hadseen certain logos on toys or other products for children and whetherthey knew what the logos said about the products. Respondents´ opinionswere sought regarding the most effective placement of safetyinstructions, whether they preferred the risks of using a toy orchildren´s product to be indicated by logos or symbols or by text, andthe usefulness of warnings appearing in text form. Finally, respondentswere asked to make judgments on the overall usefulness of safetyinformation for toys and children´s products. Demography: Age, gender, marital status, nationality, left-rightpolitical self-placement, age at completion of education, occupation,household income group, type and size of locality, and region ofresidence.");
        dataStoreClient.post(Entity.class, p2);
        
        //String thesaurus = "file:///C:/Users/domi/InFoLiS2/Verschlagwortung/thesaurus/thesoz.rdf";
        String thesaurus = "http://web.informatik.uni-mannheim.de/dominique/stw.rdf";
        
        Execution execution = new Execution();
        execution.setAlgorithm(KeywordTagger.class);
        execution.setThesaurus(thesaurus);
        execution.setEntitiesForKeywordTagging(new ArrayList<>(Arrays.asList(p.getUri(),p2.getUri())));
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        List<Keyword> detectedKeywords = dataStoreClient.get(Keyword.class, execution.getKeyWords());
        assertEquals(10,detectedKeywords.size());
//        for(Keyword k : dataStoreClient.get(Keyword.class, execution.getKeyWords())) {
//            System.out.println(k.getReferredEntity() + " --- " +k.getThesaurusLabel() + " ... " + k.getConfidenceScore());
//        }
    }
}
