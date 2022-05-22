package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);    // 생성자에서 새로 만들어줘도 되고, spring bean으로 등록해서 인젝션 받아 사용해도 됨
//        싱클톤 객체를 생성해서 사용하면 동시성 문제 발생 안함? -> yes
//        : jpaqueryfactory는 엔티티 매니저에 완전 의존. 스프링 상의 엔티티 매니저는 동시성 문제와 전혀 관계없이 트랜젝션 단위로 따로따로 분리하여 동작
//        스프링에서는 em이 진짜 영속성 컨텍스스 매니저가 아니라 proxy(가짜) 객체를 주입해줌. 그 객체가 트랜젝션 단위로 다르게 바인딩 되도록 해줌.
//        따라서 동시성 문제 없음


    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m ", Member.class).getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe((condition.getAgeLoe())));
        }


        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNmaeEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNmaeEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }


    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null ;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null ;
    }

}
