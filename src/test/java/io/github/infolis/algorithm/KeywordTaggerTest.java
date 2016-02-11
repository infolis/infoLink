package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.Keyword;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        
        String thesaurus = "file:///C:/Users/domi/InFoLiS2/Verschlagwortung/thesaurus/thesoz.rdf";
    
        Execution execution = new Execution();
        execution.setAlgorithm(KeywordTagger.class);
        execution.setThesaurus(thesaurus);
        execution.setEntitiesForKeywordTagging(new ArrayList<>(Arrays.asList(p.getUri())));
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        for(Keyword k : dataStoreClient.get(Keyword.class, execution.getKeyWords())) {
            System.out.println(k.getThesaurusLabel() + " ... " + k.getConfidenceScore());
        }
    }
}
