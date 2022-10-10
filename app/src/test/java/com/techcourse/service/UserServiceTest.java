package com.techcourse.service;

import com.techcourse.config.DataSourceConfig;
import com.techcourse.dao.UserDao;
import com.techcourse.dao.UserHistoryDao;
import com.techcourse.domain.User;
import com.techcourse.support.jdbc.init.DatabasePopulatorUtils;
import nextstep.jdbc.DataAccessException;
import nextstep.jdbc.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {

    private JdbcTemplate jdbcTemplate;
    private UserDao userDao;
    private UserHistoryDao userHistoryDao;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(DataSourceConfig.getInstance());
        userDao = new UserDao(jdbcTemplate);
        userHistoryDao = new UserHistoryDao(jdbcTemplate);

        DatabasePopulatorUtils.init(DataSourceConfig.getInstance());
    }

    @DisplayName("사용자 비밀번호 변경")
    @Test
    void changePassword() {
        final AppUserService appUserService = new AppUserService(userDao, userHistoryDao);
        userDao.insert(new User("newUser", "password", "gugu@woowahan.com"));
        final long userId = userDao.findByAccount("newUser").getId();

        appUserService.changePassword(userId, "newPassword", "gugu");

        final User actual = appUserService.findById(userId);
        assertThat(actual.getPassword()).isEqualTo("newPassword");
    }

    @DisplayName("사용자 비밀번호 변경 실패 시 롤백")
    @Test
    void changePasswordRollback() {
        // 트랜잭션 롤백 테스트를 위해 mock으로 교체
        final var userHistoryDao = new MockUserHistoryDao(jdbcTemplate);
        // 애플리케이션 서비스
        final var appUserService = new AppUserService(userDao, userHistoryDao);
        // 트랜잭션 서비스 추상화
        final var transactionManager = new DataSourceTransactionManager(jdbcTemplate.getDataSource());
        final var userService = new TxUserService(transactionManager, appUserService);

        final var user = new User("gugu", "password", "hkkang@woowahan.com");
        userDao.insert(user);
        final long userId = userDao.findByAccount("gugu").getId();

        // 트랜잭션이 정상 동작하는지 확인하기 위해 의도적으로 MockUserHistoryDao에서 예외를 발생시킨다.
        assertThrows(DataAccessException.class,
                () -> userService.changePassword(userId, "newPassword", "gugu"));

        final var actual = userService.findById(userId);

        assertThat(actual.getPassword()).isNotEqualTo("newPassword");
    }
}